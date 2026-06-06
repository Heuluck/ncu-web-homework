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
    const container = document.getElementById('toastContainer') || (() => {
      const el = document.createElement('div');
      el.id = 'toastContainer';
      el.style.cssText = 'position:fixed;top:20px;right:20px;z-index:100002;display:flex;flex-direction:column;gap:8px;pointer-events:none;';
      document.body.appendChild(el);
      return el;
    })();
    const colors = { success: 'var(--color-success)', error: 'var(--color-danger)', warning: 'var(--color-warning)', info: 'var(--color-primary)' };
    const icons = { success: 'check-circle', error: 'alert-circle', warning: 'alert-triangle', info: 'info' };
    const toast = document.createElement('div');
    toast.style.cssText = `pointer-events:auto;display:flex;align-items:center;gap:10px;padding:12px 18px;background:var(--bg-app);border:1px solid var(--border-subtle);border-left:3px solid ${colors[type] || colors.info};border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);font-size:var(--text-sm);color:var(--text-main);animation:toastSlideIn 0.25s ease-out;max-width:360px;`;
    toast.innerHTML = `<i data-lucide="${icons[type] || 'info'}" style="width:18px;height:18px;color:${colors[type] || colors.info};flex-shrink:0;"></i><span>${this.escapeHtml(message)}</span>`;
    container.appendChild(toast);
    if (typeof lucide !== 'undefined') lucide.createIcons({ nodes: [toast] });
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, 3000);
  },

  /**
   * 显示提示弹窗（替代 alert）
   * @param {string} message - 提示内容
   * @param {string} title - 标题
   * @returns {Promise<void>}
   */
  showAlert(message, title = '提示') {
    return new Promise(resolve => {
      const overlay = document.createElement('div');
      overlay.className = 'modal-overlay';
      overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;z-index:100001;animation:fadeIn 0.15s ease;';
      overlay.innerHTML = `<div class="modal-container" style="max-width:380px;width:90%;background:var(--bg-app);border-radius:var(--radius-2xl);box-shadow:var(--shadow-lg);animation:modalSlideIn 0.2s ease-out;">
        <div style="padding:var(--space-6) var(--space-6) var(--space-4);">
          <div style="font-size:var(--text-lg);font-weight:var(--font-semibold);color:var(--text-main);margin-bottom:var(--space-3);">${this.escapeHtml(title)}</div>
          <div style="font-size:var(--text-sm);color:var(--text-muted);line-height:1.6;white-space:pre-wrap;">${this.escapeHtml(message)}</div>
        </div>
        <div style="padding:0 var(--space-6) var(--space-5);display:flex;justify-content:flex-end;">
          <button class="btn btn-primary btn-sm alert-ok-btn" style="min-width:72px;">确定</button>
        </div>
      </div>`;
      const close = () => { overlay.remove(); resolve(); };
      overlay.querySelector('.alert-ok-btn').addEventListener('click', close);
      overlay.addEventListener('click', e => { if (e.target === overlay) close(); });
      const onKey = e => { if (e.key === 'Enter' || e.key === 'Escape') { e.preventDefault(); close(); document.removeEventListener('keydown', onKey); } };
      document.addEventListener('keydown', onKey);
      document.body.appendChild(overlay);
      overlay.querySelector('.alert-ok-btn').focus();
    });
  },

  /**
   * 显示确认弹窗（替代 confirm）
   * @param {string} message - 确认内容
   * @param {object} options - { title, confirmText, cancelText, danger }
   * @returns {Promise<boolean>}
   */
  showConfirm(message, options = {}) {
    const { title = '确认', confirmText = '确定', cancelText = '取消', danger = false } = options;
    return new Promise(resolve => {
      const overlay = document.createElement('div');
      overlay.className = 'modal-overlay';
      overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;z-index:100001;animation:fadeIn 0.15s ease;';
      const confirmBtnClass = danger ? 'btn btn-danger btn-sm' : 'btn btn-primary btn-sm';
      overlay.innerHTML = `<div class="modal-container" style="max-width:380px;width:90%;background:var(--bg-app);border-radius:var(--radius-2xl);box-shadow:var(--shadow-lg);animation:modalSlideIn 0.2s ease-out;">
        <div style="padding:var(--space-6) var(--space-6) var(--space-4);">
          <div style="font-size:var(--text-lg);font-weight:var(--font-semibold);color:var(--text-main);margin-bottom:var(--space-3);">${this.escapeHtml(title)}</div>
          <div style="font-size:var(--text-sm);color:var(--text-muted);line-height:1.6;white-space:pre-wrap;">${this.escapeHtml(message)}</div>
        </div>
        <div style="padding:0 var(--space-6) var(--space-5);display:flex;justify-content:flex-end;gap:var(--space-3);">
          <button class="btn btn-ghost btn-sm confirm-cancel-btn" style="min-width:72px;">${this.escapeHtml(cancelText)}</button>
          <button class="${confirmBtnClass} confirm-ok-btn" style="min-width:72px;">${this.escapeHtml(confirmText)}</button>
        </div>
      </div>`;
      let settled = false;
      const finish = val => { if (settled) return; settled = true; overlay.remove(); resolve(val); };
      overlay.querySelector('.confirm-ok-btn').addEventListener('click', () => finish(true));
      overlay.querySelector('.confirm-cancel-btn').addEventListener('click', () => finish(false));
      overlay.addEventListener('click', e => { if (e.target === overlay) finish(false); });
      const onKey = e => { if (e.key === 'Enter') { e.preventDefault(); finish(true); document.removeEventListener('keydown', onKey); } else if (e.key === 'Escape') { e.preventDefault(); finish(false); document.removeEventListener('keydown', onKey); } };
      document.addEventListener('keydown', onKey);
      document.body.appendChild(overlay);
      overlay.querySelector('.confirm-cancel-btn').focus();
    });
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
