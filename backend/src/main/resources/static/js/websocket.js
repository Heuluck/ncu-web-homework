/**
 * WebSocket 管理
 * STOMP 连接、消息订阅、自动重连
 */
const WS = {
  stompClient: null,
  connected: false,
  callbacks: {},
  reconnectAttempts: 0,
  maxReconnectAttempts: 5,
  reconnectDelay: 3000,

  /**
   * 连接 WebSocket
   * @param {string} token - JWT Token
   */
  connect(token) {
    if (this.connected) return;

    const socket = new SockJS('/ws?token=' + token);
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null; // 禁用调试日志

    this.stompClient.connect({}, 
      // 连接成功
      (frame) => {
        this.connected = true;
        this.reconnectAttempts = 0;
        console.log('WebSocket connected');

        // 订阅个人消息队列
        this.stompClient.subscribe('/user/queue/messages', (message) => {
          const data = JSON.parse(message.body);
          this.trigger('message', data);
        });

        // 订阅通知队列
        this.stompClient.subscribe('/user/queue/notifications', (notification) => {
          const data = JSON.parse(notification.body);
          this.trigger('notification', data);
        });

        // 订阅在线状态广播
        this.stompClient.subscribe('/topic/status', (status) => {
          const data = JSON.parse(status.body);
          this.trigger('status', data);
        });

        this.trigger('connected');
      }, 
      // 连接失败
      (error) => {
        console.error('WebSocket error:', error);
        this.connected = false;
        this.trigger('disconnected');
        
        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          console.log(`Reconnecting... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
          setTimeout(() => this.connect(token), this.reconnectDelay);
        }
      }
    );
  },

  /**
   * 订阅群聊消息
   * @param {number} groupId - 群 ID
   */
  subscribeGroup(groupId) {
    if (!this.stompClient || !this.connected) return;
    
    this.stompClient.subscribe(`/topic/group/${groupId}`, (message) => {
      const data = JSON.parse(message.body);
      this.trigger('groupMessage', data);
    });
  },

  /**
   * 发送消息
   * @param {string} destination - 目标地址
   * @param {object} body - 消息体
   */
  send(destination, body) {
    if (!this.stompClient || !this.connected) {
      console.error('WebSocket not connected');
      return false;
    }
    
    this.stompClient.send(destination, {}, JSON.stringify(body));
    return true;
  },

  /**
   * 断开连接
   */
  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.connected = false;
      console.log('WebSocket disconnected');
    }
  },

  /**
   * 注册事件回调
   * @param {string} event - 事件名
   * @param {Function} callback - 回调函数
   */
  on(event, callback) {
    if (!this.callbacks[event]) {
      this.callbacks[event] = [];
    }
    this.callbacks[event].push(callback);
  },

  /**
   * 移除事件回调
   * @param {string} event - 事件名
   * @param {Function} callback - 回调函数
   */
  off(event, callback) {
    if (!this.callbacks[event]) return;
    this.callbacks[event] = this.callbacks[event].filter(cb => cb !== callback);
  },

  /**
   * 触发事件
   * @param {string} event - 事件名
   * @param {*} data - 数据
   */
  trigger(event, data) {
    if (this.callbacks[event]) {
      this.callbacks[event].forEach(cb => cb(data));
    }
  },

  /**
   * 获取连接状态
   * @returns {boolean}
   */
  isConnected() {
    return this.connected;
  }
};
