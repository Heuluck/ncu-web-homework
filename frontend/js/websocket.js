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
  },

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
