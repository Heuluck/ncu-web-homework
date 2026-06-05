/**
 * 语音录制与播放模块
 * 桌面端：点击开始录音，再次点击停止并发送
 */
const VoiceManager = {
  mediaRecorder: null,
  audioChunks: [],
  isRecording: false,
  recordStartTime: null,
  recordTimer: null,
  currentAudio: null,
  playingMessageId: null,
  playedVoiceIds: new Set(),  // 已播放语音 ID 集合

  /**
   * 切换录音状态（点击切换：开始/停止）
   */
  toggleRecording() {
    if (this.isRecording) {
      this.stopRecording();
    } else {
      this.startRecording();
    }
  },

  /**
   * 开始录音
   */
  async startRecording() {
    if (this.isRecording) {
      console.warn('[Voice] Already recording, ignoring duplicate start');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.audioChunks = [];

      // 尝试多种 MIME 类型，兼容不同浏览器
      const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', ''];
      let mimeType = '';
      for (const mt of mimeTypes) {
        if (!mt || MediaRecorder.isTypeSupported(mt)) { mimeType = mt; break; }
      }
      const options = mimeType ? { mimeType } : {};
      this.mediaRecorder = new MediaRecorder(stream, options);
      console.log('[Voice] MediaRecorder created, mimeType:', this.mediaRecorder.mimeType || 'default');

      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          this.audioChunks.push(e.data);
          console.log('[Voice] dataavailable chunk:', e.data.size, 'bytes, total chunks:', this.audioChunks.length);
        }
      };

      this.mediaRecorder.onstop = async () => {
        console.log('[Voice] onstop fired, chunks:', this.audioChunks.length);
        stream.getTracks().forEach(t => t.stop());
        if (this.audioChunks.length === 0) {
          console.log('[Voice] No audio data, cancelled');
          return;
        }
        const audioBlob = new Blob(this.audioChunks, { type: this.mediaRecorder.mimeType || 'audio/webm' });
        const duration = Math.round((Date.now() - this.recordStartTime) / 1000);
        console.log('[Voice] Blob size:', audioBlob.size, 'duration:', duration, 's');
        if (duration < 1) {
          Utils.showToast('录音时间太短', 'warning');
          return;
        }
        await this._uploadAndSend(audioBlob, duration);
      };

      this.mediaRecorder.onerror = (e) => {
        console.error('[Voice] MediaRecorder error:', e);
        Utils.showToast('录音出错', 'error');
      };

      this.mediaRecorder.start();
      this.isRecording = true;
      this.recordStartTime = Date.now();
      this._startTimer();
      this._showRecordingUI(true);
      this._updateMicButton(true);
      console.log('[Voice] 开始录音, state:', this.mediaRecorder.state);
    } catch (err) {
      console.error('[Voice] 无法获取麦克风权限:', err);
      Utils.showToast('无法访问麦克风，请检查权限', 'error');
    }
  },

  /**
   * 停止录音并发送
   */
  stopRecording() {
    console.log('[Voice] stopRecording called, state:', this.mediaRecorder?.state, 'isRecording:', this.isRecording);
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.stop();
      this.isRecording = false;
      this._stopTimer();
      this._showRecordingUI(false);
      this._updateMicButton(false);
      console.log('[Voice] 停止录音，等待 onstop...');
    } else {
      console.warn('[Voice] stopRecording ignored: recorder state=' + (this.mediaRecorder?.state || 'null'));
    }
  },

  /**
   * 取消录音（清空数据，不发送）
   */
  cancelRecording() {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      this.audioChunks = [];
      this._stopTimer();
      this._showRecordingUI(false);
      this._updateMicButton(false);
      console.log('[Voice] 取消录音');
    }
  },

  /**
   * 更新麦克风按钮样式
   */
  _updateMicButton(recording) {
    const btn = document.querySelector('.chat-input-toolbar .btn-icon[title="语音"]');
    if (!btn) return;
    btn.classList.toggle('recording', recording);
    const icon = btn.querySelector('i');
    if (icon) {
      icon.setAttribute('data-lucide', recording ? 'square' : 'mic');
      lucide.createIcons({ icons: { mic: icon, square: icon } });
    }
  },

  /**
   * 上传录音并通过 WebSocket 发送
   */
  async _uploadAndSend(audioBlob, duration) {
    Utils.showToast('语音上传中...', 'info');
    const file = new File([audioBlob], `voice_${Date.now()}.webm`, { type: 'audio/webm' });
    const result = await API.upload(file);
    console.log('[Voice] Upload result:', result);
    if (!result || result.code !== 200) {
      Utils.showToast('语音上传失败: ' + (result?.message || '未知错误'), 'error');
      return;
    }
    const fileUrl = result.data.url;

    if (!WebSocketManager || !WebSocketManager.connected) {
      Utils.showToast('WebSocket 未连接，请刷新页面', 'error');
      return;
    }

    const friendId = (typeof ChatManager !== 'undefined') ? ChatManager.currentFriendId : null;
    const groupId = (typeof GroupManager !== 'undefined') ? GroupManager.currentGroupId : null;
    console.log('[Voice] friendId:', friendId, 'groupId:', groupId);

    if (friendId) {
      WebSocketManager.sendVoice(friendId, fileUrl, duration, null);
      Utils.showToast('语音已发送', 'success');
      console.log('[Voice] Sent to friend:', friendId, fileUrl, duration);
    } else if (groupId) {
      WebSocketManager.sendVoice(null, fileUrl, duration, groupId);
      Utils.showToast('语音已发送', 'success');
      console.log('[Voice] Sent to group:', groupId, fileUrl, duration);
    } else {
      console.warn('[Voice] No chat selected! friendId=', friendId, 'groupId=', groupId);
      Utils.showToast('请先点击左侧会话选择聊天对象', 'warning');
    }
  },

  /**
   * 播放语音
   */
  playAudio(messageId, fileUrl, duration) {
    // 标记为已播放，移除红点
    this._markPlayed(messageId);

    // 如果点击的是同一个正在播放的消息，则暂停
    if (this.currentAudio && this.playingMessageId === messageId) {
      this.currentAudio.pause();
      this._updatePlayIcon(messageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
      return;
    }

    // 停止正在播放的其他语音
    if (this.currentAudio) {
      this.currentAudio.pause();
      this._updatePlayIcon(this.playingMessageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
    }

    const audio = new Audio(fileUrl);
    this.currentAudio = audio;
    this.playingMessageId = messageId;

    audio.onloadedmetadata = () => {
      this._updatePlayIcon(messageId, true);
      const progressEl = document.querySelector(`.voice-progress[data-msg-id="${messageId}"]`);
      if (progressEl) {
        audio.ontimeupdate = () => {
          const pct = (audio.currentTime / audio.duration) * 100;
          progressEl.style.width = pct + '%';
        };
      }
    };

    audio.onended = () => {
      this._updatePlayIcon(messageId, false);
      const progressEl = document.querySelector(`.voice-progress[data-msg-id="${messageId}"]`);
      if (progressEl) progressEl.style.width = '0%';
      this.currentAudio = null;
      this.playingMessageId = null;
    };

    audio.onerror = () => {
      Utils.showToast('语音播放失败', 'error');
      this._updatePlayIcon(messageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
    };

    audio.play().catch(e => console.error('[Voice] 播放失败:', e));
  },

  /**
   * 渲染语音消息气泡
   */
  renderVoiceBubble(msg, isSelf) {
    const duration = msg.duration || parseInt(msg.content) || 0;
    const mins = Math.floor(duration / 60);
    const secs = duration % 60;
    const durationText = mins > 0 ? `${mins}'${String(secs).padStart(2, '0')}"` : `${secs}"`;
    const fileUrl = msg.fileUrl || msg.audioUrl || '';
    const isUnread = !isSelf && !this.playedVoiceIds.has(msg.id);

    return `
      <div class="voice-bubble ${isSelf ? 'voice-self' : ''} ${isUnread ? 'voice-unread' : ''}" data-msg-id="${msg.id}">
        ${isUnread ? '<span class="voice-unread-dot"></span>' : ''}
        <button class="voice-play-btn" onclick="VoiceManager.playAudio('${msg.id}', '${fileUrl.replace(/'/g, "\\'")}', ${duration})" title="播放语音">
          <i data-lucide="play" class="voice-play-icon"></i>
        </button>
        <div class="voice-wave">
          <div class="voice-progress" data-msg-id="${msg.id}"></div>
        </div>
        <span class="voice-duration">${durationText}</span>
      </div>
    `;
  },

  /** 标记语音为已播放 */
  _markPlayed(messageId) {
    if (!messageId) return;
    this.playedVoiceIds.add(messageId);
    const bubble = document.querySelector(`.voice-bubble[data-msg-id="${messageId}"]`);
    if (bubble) {
      bubble.classList.remove('voice-unread');
      const dot = bubble.querySelector('.voice-unread-dot');
      if (dot) dot.remove();
    }
  },

  _updatePlayIcon(messageId, isPlaying) {
    const bubble = document.querySelector(`.voice-bubble[data-msg-id="${messageId}"]`);
    if (!bubble) return;
    const icon = bubble.querySelector('.voice-play-icon');
    if (!icon) return;
    const iconName = isPlaying ? 'pause' : 'play';
    // 直接替换 SVG
    const wrapper = icon.closest('.voice-play-btn');
    if (wrapper) {
      const oldSvg = wrapper.querySelector('svg');
      if (oldSvg) oldSvg.remove();
      const newIcon = document.createElement('i');
      newIcon.setAttribute('data-lucide', iconName);
      newIcon.className = 'voice-play-icon';
      wrapper.prepend(newIcon);
      lucide.createIcons();
    }
  },

  _startTimer() {
    const el = document.getElementById('voice-recording-timer');
    if (!el) return;
    this.recordTimer = setInterval(() => {
      const elapsed = Math.round((Date.now() - this.recordStartTime) / 1000);
      el.textContent = `${elapsed}s`;
      // 超过 60 秒自动停止
      if (elapsed >= 60) {
        this.stopRecording();
        Utils.showToast('录音已达最大时长', 'info');
      }
    }, 200);
  },

  _stopTimer() {
    if (this.recordTimer) {
      clearInterval(this.recordTimer);
      this.recordTimer = null;
    }
  },

  _showRecordingUI(show) {
    let el = document.getElementById('voice-recording-indicator');
    if (!el) {
      el = document.createElement('div');
      el.id = 'voice-recording-indicator';
      el.className = 'voice-recording-indicator';
      el.innerHTML = `
        <div class="voice-recording-dot"></div>
        <span id="voice-recording-timer">0s</span>
        <span class="voice-recording-hint">录音中...</span>
        <button class="btn btn-success btn-sm" onclick="VoiceManager.stopRecording()" title="停止并发送" style="margin-left:8px;">
          <i data-lucide="send" style="width:14px;height:14px;"></i> 发送
        </button>
        <button class="btn btn-danger btn-sm" onclick="VoiceManager.cancelRecording()" title="取消录音" style="margin-left:4px;">
          <i data-lucide="x" style="width:14px;height:14px;"></i> 取消
        </button>
      `;
      document.body.appendChild(el);
    }
    el.classList.toggle('active', show);
    if (show) {
      lucide.createIcons();
    }
  }
};
