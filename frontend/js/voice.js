/**
 * 语音录制与播放模块
 * 桌面端：点击开始录音，再次点击停止并发送
 */
const VoiceManager = {
  mediaRecorder: null,
  audioChunks: [],
  isRecording: false,
  cancelled: false,
  recordStartTime: null,
  recordTimer: null,
  currentAudio: null,
  playingMessageId: null,
  playedVoiceIds: new Set(),  // 已播放语音 ID 集合

  /** 初始化：从 localStorage 恢复已播放状态 */
  init() {
    try {
      const stored = localStorage.getItem('voice_played_ids');
      if (stored) {
        const ids = JSON.parse(stored);
        if (Array.isArray(ids)) {
          this.playedVoiceIds = new Set(ids.slice(-200)); // 最多保留 200 条
        }
      }
    } catch (e) {
      console.warn('[Voice] 无法恢复已播放状态:', e);
    }
  },

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
    // 立即标记为录音中，防止 async 等待期间重复点击
    this.isRecording = true;
    this.cancelled = false;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      // 再次检查：可能在等待权限期间被取消了
      if (!this.isRecording || this.cancelled) {
        stream.getTracks().forEach(t => t.stop());
        console.log('[Voice] 录音已在权限等待期间取消');
        return;
      }
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
        console.log('[Voice] onstop fired, chunks:', this.audioChunks.length, 'cancelled:', this.cancelled);
        stream.getTracks().forEach(t => t.stop());
        if (this.cancelled || this.audioChunks.length === 0) {
          console.log('[Voice] No audio data or cancelled, skip upload');
          this.cancelled = false;
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
      this.recordStartTime = Date.now();
      // 先显示 UI（创建计时器元素），再启动计时器
      this._showRecordingUI(true);
      this._startTimer();
      this._updateMicButton(true);
      console.log('[Voice] 开始录音, state:', this.mediaRecorder.state);
    } catch (err) {
      this.isRecording = false;
      this.cancelled = false;
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
      this.cancelled = false;
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
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.cancelled = true;
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

    // 停止并重置正在播放的其他语音
    if (this.currentAudio) {
      this.currentAudio.pause();
      this._resetProgress(this.playingMessageId);
      this._updatePlayIcon(this.playingMessageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
    }

    const audio = new Audio(fileUrl);
    this.currentAudio = audio;
    this.playingMessageId = messageId;

    // 设置进度条更新：先绑定 timeupdate，再在回调内判断 duration
    // WebM 短语音可能 duration 为 Infinity，不能在绑定阶段就拦截
    const setupProgress = () => {
      const progressEl = document.querySelector(`.voice-progress[data-msg-id="${messageId}"]`);
      if (!progressEl) return;
      audio.ontimeupdate = () => {
        if (audio.duration && isFinite(audio.duration)) {
          const pct = (audio.currentTime / audio.duration) * 100;
          progressEl.style.width = pct + '%';
        }
      };
    };

    // 元数据加载后更新播放图标并绑定进度条
    audio.onloadedmetadata = () => {
      this._updatePlayIcon(messageId, true);
      setupProgress();
    };

    // 处理缓存命中：元数据已就绪时 loadedmetadata 不会再触发
    if (audio.readyState >= 1) {
      this._updatePlayIcon(messageId, true);
      setupProgress();
    }

    audio.onended = () => {
      this._resetProgress(messageId);
      this._updatePlayIcon(messageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
    };

    audio.onerror = () => {
      Utils.showToast('语音播放失败', 'error');
      this._resetProgress(messageId);
      this._updatePlayIcon(messageId, false);
      this.currentAudio = null;
      this.playingMessageId = null;
    };

    audio.play().catch(e => console.error('[Voice] 播放失败:', e));
  },

  /** 重置进度条 */
  _resetProgress(messageId) {
    const progressEl = document.querySelector(`.voice-progress[data-msg-id="${messageId}"]`);
    if (progressEl) progressEl.style.width = '0%';
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
    // 持久化到 localStorage
    try {
      const ids = [...this.playedVoiceIds].slice(-200);
      localStorage.setItem('voice_played_ids', JSON.stringify(ids));
    } catch (e) {
      console.warn('[Voice] 保存已播放状态失败:', e);
    }
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
    const btn = bubble.querySelector('.voice-play-btn');
    if (!btn) return;
    const iconName = isPlaying ? 'pause' : 'play';
    // 完全清空按钮并重建图标，避免 Lucide 替换 <i> 为 SVG 后 querySelector 找不到元素
    btn.innerHTML = '';
    const icon = document.createElement('i');
    icon.setAttribute('data-lucide', iconName);
    icon.className = 'voice-play-icon';
    btn.appendChild(icon);
    lucide.createIcons();
  },

  _startTimer() {
    // 清除旧定时器，防止重复
    this._stopTimer();
    const el = document.getElementById('voice-recording-timer');
    if (!el) return;
    this.recordTimer = setInterval(() => {
      // 如果已不在录音状态，停止定时器
      if (!this.isRecording) {
        this._stopTimer();
        return;
      }
      const elapsed = Math.round((Date.now() - this.recordStartTime) / 1000);
      el.textContent = `${elapsed}s`;
      // 超过 60 秒自动停止
      if (elapsed >= 60) {
        this._stopTimer();
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
