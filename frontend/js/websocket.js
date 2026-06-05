/**
 * WebSocket 连接管理
 * 基于 SockJS + STOMP.js，管理连接、订阅、自动重连
 */
const WebSocketManager = {
  stompClient: null,
  connected: false,
  reconnectAttempts: 0,
  maxReconnectAttempts: 10,
  reconnectDelay: 3000,
  _messageCallbacks: [],
  _statusCallbacks: [],
  _connectCallbacks: [],
  _disconnectCallbacks: [],
  _groupMessageCallbacks: [],  // 群聊消息回调数组
  _voiceMessageCallbacks: [],  // 语音消息回调数组
  _callSignalCallbacks: [],    // 通话信令回调数组
  _friendRequestCallbacks: [], // 好友申请回调数组

  connect() {
    const token = Auth.getToken();
    if (!token) {
      console.warn('[WS] 无 Token，跳过连接');
      return;
    }

    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;

    const headers = { 'Authorization': `Bearer ${token}` };

    this.stompClient.connect(headers,
        () => {
          console.log('[WS] 连接成功');
          this.connected = true;
          this.reconnectAttempts = 0;
          this._subscribeAll();
          this._connectCallbacks.forEach(cb => cb());
        },
        (error) => {
          console.error('[WS] 连接失败:', error);
          this.connected = false;
          this._disconnectCallbacks.forEach(cb => cb(error));
          this._attemptReconnect();
        }
    );
  },

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        console.log('[WS] 已断开连接');
        this.connected = false;
      });
    }
  },

  _subscribeAll() {
    if (!this.stompClient || !this.connected) return;

    this.stompClient.subscribe('/user/queue/messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        this._messageCallbacks.forEach(cb => cb(data));
      } catch (e) {
        console.error('[WS] 消息解析失败:', e);
      }
    });

    this.stompClient.subscribe('/topic/status', (message) => {
      try {
        const data = JSON.parse(message.body);
        this._statusCallbacks.forEach(cb => cb(data));
      } catch (e) {
        console.error('[WS] 状态消息解析失败:', e);
      }
    });

    // 添加群聊消息订阅
    this.stompClient.subscribe('/user/queue/group_messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (this._groupMessageCallbacks) {
          this._groupMessageCallbacks.forEach(cb => cb(data));
        }
      } catch (e) {
        console.error('[WS] 群消息解析失败:', e);
      }
    });
    // 好友拉黑/解除状态变更通知
    this.stompClient.subscribe('/user/queue/friend-status', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (data.type === 'BLOCK_STATUS_CHANGE' && typeof FriendManager !== 'undefined') {
          FriendManager.onBlockStatusChange(data);
        }
      } catch (e) { console.error('[WS] 好友状态解析失败:', e); }
    });

    // 语音消息订阅
    this.stompClient.subscribe('/user/queue/voice_messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (this._voiceMessageCallbacks) {
          this._voiceMessageCallbacks.forEach(cb => cb(data));
        }
      } catch (e) {
        console.error('[WS] 语音消息解析失败:', e);
      }
    });

    // 通话信令订阅
    this.stompClient.subscribe('/user/queue/call', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (this._callSignalCallbacks) {
          this._callSignalCallbacks.forEach(cb => cb(data));
        }
      } catch (e) {
        console.error('[WS] 通话信令解析失败:', e);
      }
    });

    // 好友申请实时通知
    this.stompClient.subscribe('/user/queue/friend-request', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (this._friendRequestCallbacks) {
          this._friendRequestCallbacks.forEach(cb => cb(data));
        }
      } catch (e) {
        console.error('[WS] 好友申请解析失败:', e);
      }
    });
    // 群事件订阅（解散、移除成员等）
    this.stompClient.subscribe('/user/queue/group_events', (message) => {
      try {
        const data = JSON.parse(message.body);
        if (typeof GroupManager !== 'undefined') {
          GroupManager.onGroupEvent(data);
        }
      } catch (e) {
        console.error('[WS] 群事件解析失败:', e);
      }
    });  },

  sendMessage(to, content, messageType = 0, fileUrl = null) {
    if (!this.stompClient || !this.connected) {
      console.error('[WS] 未连接，无法发送消息');
      return false;
    }
    this.stompClient.send('/app/chat.send', {}, JSON.stringify({
      to, content, messageType, fileUrl
    }));
    return true;
  },

  sendReadNotification(friendId) {
    if (!this.stompClient || !this.connected) return;
    this.stompClient.send('/app/chat.read', {}, JSON.stringify({ friendId }));
  },

  onMessage(cb) { this._messageCallbacks.push(cb); },
  onStatusChange(cb) { this._statusCallbacks.push(cb); },
  onConnected(cb) { this._connectCallbacks.push(cb); },
  onDisconnected(cb) { this._disconnectCallbacks.push(cb); },

  // 注册群聊消息回调
  onGroupMessage(cb) {
    this._groupMessageCallbacks.push(cb);
  },

  // 注册语音消息回调
  onVoiceMessage(cb) {
    this._voiceMessageCallbacks.push(cb);
  },

  // 注册通话信令回调
  onCallSignal(cb) {
    this._callSignalCallbacks.push(cb);
  },

  // 注册好友申请回调
  onFriendRequest(cb) {
    this._friendRequestCallbacks.push(cb);
  },

  // 发送群聊消息
  sendGroupMessage(groupId, content, messageType = 0, fileUrl = null) {
    if (!this.stompClient || !this.connected) {
      console.error('[WS] 未连接');
      return false;
    }
    this.stompClient.send('/app/group.send', {}, JSON.stringify({
      groupId, content, messageType, fileUrl
    }));
    return true;
  },

  // 发送语音消息（WebSocket）
  sendVoice(to, audioUrl, duration, groupId = null) {
    if (!this.stompClient || !this.connected) {
      console.error('[WS] 未连接，无法发送语音');
      return false;
    }
    this.stompClient.send('/app/voice.send', {}, JSON.stringify({
      to: to,
      groupId: groupId,
      fileUrl: audioUrl,
      duration: duration
    }));
    return true;
  },

  // 发送通话信令
  sendCallSignal(destination, payload) {
    if (!this.stompClient || !this.connected) return false;
    this.stompClient.send('/app/call.' + payload.type, {}, JSON.stringify(payload));
    return true;
  },

  _attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WS] 重连次数超限，停止重连');
      return;
    }
    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1);
    console.log(`[WS] ${delay / 1000}s 后尝试第 ${this.reconnectAttempts} 次重连...`);
    setTimeout(() => {
      if (!this.connected) this.connect();
    }, delay);
  },

  isConnected() { return this.connected; }
};