/**
 * 工具函数
 * 时间格式化、HTML 转义、头像处理等
 */
const Utils = {
  /**
   * 格式化时间
   * @param {string} dateStr - 日期字符串
   * @returns {string} 格式化后的时间
   */
  formatTime(dateStr) {
    if (!dateStr) return '';
    
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now - date;
    
    // 1 分钟内
    if (diff < 60000) return '刚刚';
    // 1 小时内
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    // 24 小时内
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    // 今年
    if (year === now.getFullYear()) {
      return `${month}-${day} ${hours}:${minutes}`;
    }
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  },

  /**
   * 格式化消息时间（用于消息气泡）
   * @param {string} dateStr - 日期字符串
   * @returns {string} HH:mm 格式
   */
  formatMessageTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  },

  /**
   * HTML 转义（防 XSS）
   * @param {string} text - 原始文本
   * @returns {string} 转义后的文本
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  },

  /**
   * 获取头像 URL
   * @param {string} avatar - 头像地址
   * @param {string} seed - DiceBear 种子
   * @returns {string} 头像 URL
   */
  getAvatarUrl(avatar, seed) {
    if (avatar) return avatar;
    return `https://api.dicebear.com/7.x/avataaars/svg?seed=${seed || 'default'}`;
  },

  /**
   * 获取状态文本
   * @param {number} status - 状态码
   * @returns {string} 状态文本
   */
  getStatusText(status) {
    const statusMap = {
      0: '离线',
      1: '在线',
      2: '忙碌',
      3: '勿扰'
    };
    return statusMap[status] || '离线';
  },

  /**
   * 获取状态类名
   * @param {number} status - 状态码
   * @returns {string} CSS 类名
   */
  getStatusClass(status) {
    const classMap = {
      0: 'offline',
      1: 'online',
      2: 'busy',
      3: 'dnd'
    };
    return classMap[status] || 'offline';
  },

  /**
   * 显示提示消息
   * @param {string} message - 消息内容
   * @param {string} type - 类型：success, error, warning, info
   */
  showToast(message, type = 'info') {
    // TODO: 实现 toast 提示
    console.log(`[${type.toUpperCase()}] ${message}`);
  },

  /**
   * 防抖函数
   * @param {Function} fn - 要执行的函数
   * @param {number} delay - 延迟时间
   * @returns {Function}
   */
  debounce(fn, delay = 300) {
    let timer = null;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  },

  /**
   * 节流函数
   * @param {Function} fn - 要执行的函数
   * @param {number} limit - 间隔时间
   * @returns {Function}
   */
  throttle(fn, limit = 300) {
    let inThrottle = false;
    return function (...args) {
      if (!inThrottle) {
        fn.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }
};
