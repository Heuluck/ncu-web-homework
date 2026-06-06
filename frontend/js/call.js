/**
 * 语音通话管理模块 — WebRTC 集成
 * 桌面端：点击开始/停止录音，非长按
 */

/** 通话提示音生成器（Web Audio API） */
const CallTone = {
  _ctx: null,
  _osc: null,
  _gain: null,
  _interval: null,

  _ensureCtx() {
    if (!this._ctx) this._ctx = new (window.AudioContext || window.webkitAudioContext)();
    return this._ctx;
  },

  /** 呼叫方听到的等待音：每 2 秒一声「嘟」 */
  startRingback() {
    this.stop();
    const ctx = this._ensureCtx();
    const playBeep = () => {
      if (!this._interval) return; // 已停止
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = 'sine';
      osc.frequency.value = 440;
      gain.gain.value = 0.15;
      osc.start();
      osc.stop(ctx.currentTime + 0.3);
    };
    playBeep();
    this._interval = setInterval(playBeep, 2000);
  },

  /** 被叫方听到的来电铃声：播放 sound.mp3 循环 */
  startRing() {
    this.stop();
    this._audio = new Audio('/sound.mp3');
    this._audio.loop = true;
    this._audio.play().catch(e => console.warn('[CallTone] 播放铃声失败:', e));
  },

  stop() {
    if (this._interval) { clearInterval(this._interval); this._interval = null; }
    if (this._audio) { this._audio.pause(); this._audio.currentTime = 0; this._audio = null; }
  }
};

const CallManager = {
  callState: 'IDLE', // IDLE | CALLING | RINGING | ONGOING
  partnerId: null,
  partnerName: '',
  partnerAvatar: '',
  callStartTime: null,
  callTimer: null,
  isMuted: false,
  isSpeakerMuted: false,
  pendingOfferSdp: null, // 暂存来电的 SDP Offer
  pendingIceCandidates: [], // 暂存 PC 创建前到达的 ICE 候选
  // WebRTC
  pc: null,
  localStream: null,
  remoteAudio: null,
  // Volume visualization
  _volCtx: null,
  _micAnalyser: null,
  _speakerAnalyser: null,
  _volRaf: null,

  /** STUN/TURN 服务器配置 — 优先使用自建 coturn */
  RTC_CONFIG: {
    iceServers: [{
      urls: [
        'stun:' + (window.location.hostname) + ':3478',
        'turn:' + (window.location.hostname) + ':3478'
      ],
      username: 'chat',
      credential: 'chat-turn-password'
    }],
    iceCandidatePoolSize: 2
  },

  /**
   * 初始化通话信令监听
   */
  init() {
    WebSocketManager.onCallSignal((signal) => {
      this._handleSignal(signal);
    });
    WebSocketManager.onDisconnected(() => {
      if (this.callState !== 'IDLE') {
        this._endCall('网络已断开');
      }
    });
  },

  /**
   * 发起语音通话
   */
  async startCall(userId, userName, userAvatar) {
    if (this.callState !== 'IDLE') {
      Utils.showToast('当前无法发起通话', 'warning');
      return;
    }
    // 防抖：立即标记为 CALLING，避免双击/重复绑定导致双重调用
    this.callState = 'CALLING';
    this.partnerId = userId;
    this.partnerName = userName;
    this.partnerAvatar = userAvatar;
    this._showCallUI('calling');
    CallTone.startRingback();  // 呼叫方等待音

    try {
      // 获取本地音频流
      this.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
      this._createMicVisualizer(this.localStream);

      // 创建 PeerConnection
      await this._createPeerConnection();

      // 添加本地音轨
      this.localStream.getTracks().forEach(track => {
        this.pc.addTrack(track, this.localStream);
      });

      // 创建 SDP Offer
      const offer = await this.pc.createOffer();
      await this.pc.setLocalDescription(offer);

      // 发送 SDP Offer 给被叫
      WebSocketManager.sendCallSignal(userId, {
        type: 'offer',
        calleeId: userId,
        sdp: this.pc.localDescription.sdp
      });

      console.log('[WebRTC] Offer sent to', userId);
    } catch (err) {
      console.error('[WebRTC] startCall error:', err);
      Utils.showToast('无法访问麦克风或建立连接', 'error');
      this._endCall('通话失败');
    }
  },

  /**
   * 创建 RTCPeerConnection
   */
  async _createPeerConnection() {
    if (this.pc) {
      this.pc.close();
      this.pc = null;
    }

    this.pc = new RTCPeerConnection(this.RTC_CONFIG);

    // ICE Candidate 事件：发送给对方
    this.pc.onicecandidate = (event) => {
      if (event.candidate) {
        WebSocketManager.sendCallSignal(this.partnerId, {
          type: 'ice',
          calleeId: this.partnerId,
          candidate: JSON.stringify(event.candidate)
        });
      }
    };

    // 远端音轨：创建 Audio 元素播放 + 音量分析器
    this.pc.ontrack = (event) => {
      console.log('[WebRTC] Remote track received');
      if (this.remoteAudio) this.remoteAudio.remove();
      this._createSpeakerVisualizer(event.streams[0]);
      this.remoteAudio = new Audio();
      this.remoteAudio.volume = 1;
      this.remoteAudio.srcObject = event.streams[0];
      document.body.appendChild(this.remoteAudio);
      // 部分浏览器需要显式 play()+用户手势才能播放音频
      const playPromise = this.remoteAudio.play();
      if (playPromise) {
        playPromise.catch(e => console.warn('[WebRTC] remoteAudio.play() failed:', e));
      }
    };

    // ICE 连接状态变化
    this.pc.oniceconnectionstatechange = () => {
      console.log('[WebRTC] ICE state:', this.pc.iceConnectionState);
      if (this.pc.iceConnectionState === 'connected') {
        this._updateCallStatus('已连接');
      }
      if (this.pc.iceConnectionState === 'disconnected' ||
          this.pc.iceConnectionState === 'failed') {
        this._endCall('连接断开');
      }
    };
  },

  /** 确保 AudioContext 存在且处于 running 状态 */
  _ensureVolCtx() {
    if (!this._volCtx) {
      this._volCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (this._volCtx.state === 'suspended') {
      this._volCtx.resume();
    }
  },

  /** 为本地的麦克风创建音量分析器 */
  _createMicVisualizer(stream) {
    if (!stream || this._micAnalyser) return;
    this._ensureVolCtx();
    const source = this._volCtx.createMediaStreamSource(stream);
    this._micAnalyser = this._volCtx.createAnalyser();
    this._micAnalyser.fftSize = 256;
    source.connect(this._micAnalyser);
    const gain = this._volCtx.createGain();
    gain.gain.value = 0;
    this._micAnalyser.connect(gain);
    gain.connect(this._volCtx.destination);
    this._startVolumeAnimation();
  },

  /** 为对方的扬声器（远端音频流）创建音量分析器 */
  _createSpeakerVisualizer(stream) {
    if (!stream || this._speakerAnalyser) return;
    this._ensureVolCtx();
    const source = this._volCtx.createMediaStreamSource(stream);
    this._speakerAnalyser = this._volCtx.createAnalyser();
    this._speakerAnalyser.fftSize = 256;
    source.connect(this._speakerAnalyser);
    const gain = this._volCtx.createGain();
    gain.gain.value = 0;
    this._speakerAnalyser.connect(gain);
    gain.connect(this._volCtx.destination);
    this._startVolumeAnimation();
  },

  /** 启动音量动画循环 */
  _startVolumeAnimation() {
    if (this._volRaf) return;
    const animate = () => {
      this._updateVolumeUI();
      this._volRaf = requestAnimationFrame(animate);
    };
    animate();
  },

  /** 停止音量动画循环 */
  _stopVolumeAnimation() {
    if (this._volRaf) {
      cancelAnimationFrame(this._volRaf);
      this._volRaf = null;
    }
  },

  /** 计算 AnalyserNode 的 RMS 音量（0~1） */
  _getVolumeLevel(analyser) {
    if (!analyser) return 0;
    const data = new Uint8Array(analyser.frequencyBinCount);
    analyser.getByteTimeDomainData(data);
    let sumSquares = 0;
    for (let i = 0; i < data.length; i++) {
      const normalized = (data[i] - 128) / 128;
      sumSquares += normalized * normalized;
    }
    const rms = Math.sqrt(sumSquares / data.length);
    return Math.min(1, rms * 3);
  },

  /** 更新音量 UI 条 */
  _updateVolumeUI() {
    const micLevel = this._getVolumeLevel(this._micAnalyser);
    const speakerLevel = this._getVolumeLevel(this._speakerAnalyser);
    const micFill = document.getElementById('mic-vol-fill');
    const speakerFill = document.getElementById('speaker-vol-fill');
    if (micFill) micFill.style.width = (micLevel * 100) + '%';
    if (speakerFill) speakerFill.style.width = (speakerLevel * 100) + '%';
  },

  /** 刷新缓存的 ICE 候选（在 remoteDescription 就绪后调用） */
  async _flushPendingIceCandidates() {
    if (this.pendingIceCandidates.length === 0) return;
    const candidates = [...this.pendingIceCandidates];
    this.pendingIceCandidates = [];
    for (const c of candidates) {
      try {
        await this.pc.addIceCandidate(new RTCIceCandidate(JSON.parse(c)));
      } catch (e) {
        console.warn('[WebRTC] Flush pending ICE error:', e);
      }
    }
  },

  /**
   * 处理通话信令
   */
  async _handleSignal(signal) {
    switch (signal.type) {
      case 'CALL_OFFER':
        if (this.callState !== 'IDLE') {
          WebSocketManager.sendCallSignal(signal.callerId, {
            type: 'reject', calleeId: signal.callerId, reason: 'busy'
          });
          return;
        }
        this.partnerId = signal.callerId;
        this.partnerName = signal.callerName || '未知用户';
        this.partnerAvatar = signal.callerAvatar || '';
        this.pendingOfferSdp = signal.sdp || null; // 存储 Offer SDP
        this.callState = 'RINGING';
        this._showCallUI('ringing');
        CallTone.startRing();
        console.log('[WebRTC] Received offer, pending SDP:', !!this.pendingOfferSdp);
        break;

      case 'CALL_ANSWER':
        CallTone.stop();  // 停止等待音
        this.callState = 'ONGOING';
        this.callStartTime = Date.now();
        this._startCallTimer();
        this._updateCallUI('ongoing');
        if (signal.sdp && this.pc) {
          try {
            await this.pc.setRemoteDescription(
              new RTCSessionDescription({ type: 'answer', sdp: signal.sdp })
            );
            await this._flushPendingIceCandidates();
          } catch (e) {
            console.error('[WebRTC] setRemoteDescription error:', e);
          }
        }
        break;

      case 'CALL_ICE':
        if (signal.candidate) {
          if (this.pc && this.pc.remoteDescription) {
            try {
              const candidate = JSON.parse(signal.candidate);
              await this.pc.addIceCandidate(new RTCIceCandidate(candidate));
            } catch (e) {
              console.error('[WebRTC] addIceCandidate error:', e);
            }
          } else {
            this.pendingIceCandidates.push(signal.candidate);
            console.log('[WebRTC] ICE candidate buffered (remote desc not ready)');
          }
        }
        break;

      case 'CALL_REJECT':
        this._endCall(signal.reason === 'busy' ? '对方正在通话中' : '对方拒绝了通话');
        break;

      case 'CALL_HANGUP':
        this._endCall('对方已挂断', signal.duration);
        break;

      case 'CALL_CANCEL':
        this._endCall('对方取消了通话');
        break;
    }
  },

  /**
   * 接听
   */
  async answer() {
    if (this.callState !== 'RINGING') return;
    CallTone.stop();  // 停止来电铃声

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
      this._createMicVisualizer(this.localStream);
      await this._createPeerConnection();
      this.localStream.getTracks().forEach(track => this.pc.addTrack(track, this.localStream));

      // 1. 设置远端 SDP（来自 Offer）
      if (this.pendingOfferSdp && this.pc) {
        await this.pc.setRemoteDescription(
          new RTCSessionDescription({ type: 'offer', sdp: this.pendingOfferSdp })
        );
        console.log('[WebRTC] Remote description set from offer');
        await this._flushPendingIceCandidates();
      }

      // 2. 创建 Answer
      const answer = await this.pc.createAnswer();
      await this.pc.setLocalDescription(answer);
      console.log('[WebRTC] Answer created');

      // 3. 发送真实 SDP Answer
      WebSocketManager.sendCallSignal(this.partnerId, {
        type: 'answer',
        calleeId: this.partnerId,
        sdp: this.pc.localDescription.sdp
      });

      this.callState = 'ONGOING';
      this.callStartTime = Date.now();
      this._startCallTimer();
      this._updateCallUI('ongoing');
      this.pendingOfferSdp = null;
      console.log('[WebRTC] Answer sent to', this.partnerId);
    } catch (err) {
      console.error('[WebRTC] answer error:', err);
      Utils.showToast('无法访问麦克风', 'error');
      this.reject();
    }
  },

  reject() {
    WebSocketManager.sendCallSignal(this.partnerId, {
      type: 'reject', calleeId: this.partnerId, reason: 'decline'
    });
    this._endCall('已拒绝');
  },

  hangup() {
    if (this.callState === 'CALLING') {
      WebSocketManager.sendCallSignal(this.partnerId, { type: 'cancel', calleeId: this.partnerId });
      this._endCall('已取消');
    } else {
      const duration = this.callStartTime
        ? Math.round((Date.now() - this.callStartTime) / 1000) : 0;
      WebSocketManager.sendCallSignal(this.partnerId, {
        type: 'hangup', calleeId: this.partnerId, duration: duration
      });
    }
    this._endCall('通话结束');
  },

  toggleMute() {
    this.isMuted = !this.isMuted;
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach(track => { track.enabled = !this.isMuted; });
    }
    const btn = document.getElementById('call-mute-btn');
    if (btn) {
      btn.innerHTML = this.isMuted
        ? '<i data-lucide="mic-off"></i>' : '<i data-lucide="mic"></i>';
      if (window.lucide) lucide.createIcons();
    }
    Utils.showToast(this.isMuted ? '已静音' : '已取消静音', 'info');
  },

  toggleSpeakerMute() {
    if (!this.remoteAudio) return;
    this.isSpeakerMuted = !this.isSpeakerMuted;
    this.remoteAudio.muted = this.isSpeakerMuted;
    const btn = document.getElementById('call-speaker-btn');
    if (btn) {
      btn.innerHTML = this.isSpeakerMuted
        ? '<i data-lucide="volume-x"></i>' : '<i data-lucide="volume-2"></i>';
      if (window.lucide) lucide.createIcons();
    }
    Utils.showToast(this.isSpeakerMuted ? '听筒已静音' : '听筒已取消静音', 'info');
  },

  _endCall(message, duration) {
    CallTone.stop();
    const callDuration = duration || (
      this.callStartTime ? Math.round((Date.now() - this.callStartTime) / 1000) : 0
    );
    this._stopCallTimer();
    this._stopVolumeAnimation();
    this.callState = 'IDLE';

    if (this.pc) { this.pc.close(); this.pc = null; }
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }
    if (this.remoteAudio) { this.remoteAudio.remove(); this.remoteAudio = null; }
    this._micAnalyser = null;
    this._speakerAnalyser = null;
    if (this._volCtx) { this._volCtx.close(); this._volCtx = null; }

    // 立即更新 overlay 文案，移除操作按钮
    if (message) {
      const el = document.getElementById('call-overlay');
      if (el) {
        const statusEl = el.querySelector('.call-status');
        if (statusEl) { statusEl.textContent = message; statusEl.style.display = ''; }
        const timerEl = el.querySelector('.call-timer');
        if (timerEl) timerEl.style.display = 'none';
        const actionsEl = el.querySelector('.call-actions');
        if (actionsEl) actionsEl.style.display = 'none';
      }
      const durationText = callDuration > 0 ? ` (${this._formatDuration(callDuration)})` : '';
      Utils.showToast(message + durationText, 'info');
    }

    setTimeout(() => {
      const el = document.getElementById('call-overlay');
      if (el) el.remove();
    }, 1500);

    this.partnerId = null;
    this.callStartTime = null;
    this.isSpeakerMuted = false;
    this.pendingOfferSdp = null;
    this.pendingIceCandidates = [];
  },

  _showCallUI(mode) {
    let el = document.getElementById('call-overlay');
    if (el) el.remove();
    el = document.createElement('div');
    el.id = 'call-overlay';
    el.className = 'call-overlay';
    const avatarUrl = this.partnerAvatar || Utils.getAvatarUrl(null, this.partnerName);

    if (mode === 'ringing') {
      el.innerHTML = `<div class="call-container ringing"><div class="call-ripple"></div><img class="call-avatar" src="${avatarUrl}" alt="" onerror="this.src='${Utils.getAvatarUrl(null, 'user')}'"><div class="call-name">${Utils.escapeHtml(this.partnerName)}</div><div class="call-status">邀请你语音通话</div><div class="call-actions"><button class="call-btn call-btn-reject" onclick="CallManager.reject()"><i data-lucide="phone-off"></i></button><button class="call-btn call-btn-answer" onclick="CallManager.answer()"><i data-lucide="phone"></i></button></div></div>`;
    } else if (mode === 'calling') {
      el.innerHTML = `<div class="call-container calling"><img class="call-avatar" src="${avatarUrl}" alt="" onerror="this.src='${Utils.getAvatarUrl(null, 'user')}'"><div class="call-name">${Utils.escapeHtml(this.partnerName)}</div><div class="call-status" id="call-status-text">正在呼叫...</div><div class="call-actions"><button class="call-btn call-btn-reject" onclick="CallManager.hangup()"><i data-lucide="phone-off"></i></button></div></div>`;
    } else if (mode === 'ongoing') {
      el.innerHTML = `<div class="call-container ongoing"><img class="call-avatar call-avatar-ongoing" src="${avatarUrl}" alt="" onerror="this.src='${Utils.getAvatarUrl(null, 'user')}'"><div class="call-name">${Utils.escapeHtml(this.partnerName)}</div><div class="call-status" id="call-status-text" style="display:none"></div><div class="call-timer" id="call-timer">00:00</div><div class="call-volume-row"><div class="vol-item"><i data-lucide="mic" class="vol-icon"></i><div class="vol-track"><div class="vol-fill" id="mic-vol-fill"></div></div></div><div class="vol-item"><i data-lucide="volume-2" class="vol-icon" id="speaker-vol-icon"></i><div class="vol-track"><div class="vol-fill" id="speaker-vol-fill"></div></div></div></div><div class="call-actions"><button class="call-btn call-btn-mute" id="call-mute-btn" onclick="CallManager.toggleMute()"><i data-lucide="mic"></i></button><button class="call-btn call-btn-mute" id="call-speaker-btn" onclick="CallManager.toggleSpeakerMute()"><i data-lucide="volume-2"></i></button><button class="call-btn call-btn-reject" onclick="CallManager.hangup()"><i data-lucide="phone-off"></i></button></div></div>`;
    }

    document.body.appendChild(el);
    if (window.lucide) lucide.createIcons();
  },

  _updateCallUI(mode) { this._showCallUI(mode); },

  _updateCallStatus(text) {
    const el = document.getElementById('call-status-text');
    if (el) el.textContent = text;
  },

  _startCallTimer() {
    this._stopCallTimer();
    this.callTimer = setInterval(() => {
      const elapsed = Math.round((Date.now() - this.callStartTime) / 1000);
      const el = document.getElementById('call-timer');
      if (el) el.textContent = this._formatDuration(elapsed);
    }, 500);
  },

  _stopCallTimer() {
    if (this.callTimer) { clearInterval(this.callTimer); this.callTimer = null; }
  },

  _formatDuration(seconds) {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
};
