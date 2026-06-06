/**
 * 表情选择器组件
 */
const EmojiPicker = {
  emojis: [],
  currentCategory: 'default',
  onSelectCallback: null,
  _closeTimer: null,
  _closeHandler: null,
  _rendering: false,

  // 初始化：加载表情列表
  async init() {
    try {
      const result = await API.get('/api/emoji/list');
      console.log('[表情] 加载结果:', result);
      if (result && result.code === 200) {
        this.emojis = result.data || [];
        console.log('[表情] 表情数量:', this.emojis.length);
      }
    } catch (error) {
      console.error('[表情] 加载失败:', error);
    }
  },

  // 获取表情 HTML
  render() {
    if (!this.emojis || this.emojis.length === 0) {
      return '<div class="emoji-empty">暂无表情</div>';
    }

    let html = '<div class="emoji-grid">';
    this.emojis.forEach(emoji => {
      html += '<div class="emoji-item" data-emoji="' + emoji.url + '" title="' + Utils.escapeHtml(emoji.name) + '"><span style="font-size:24px;">' + emoji.url + '</span></div>';
    });
    html += '</div>';
    return html;
  },

  // 显示表情选择器
  show(containerId, onSelect) {
    const container = document.getElementById(containerId);
    if (!container) {
      console.error('[表情] 容器不存在:', containerId);
      return;
    }

    // 防止重复渲染
    if (this._rendering) return;
    this._rendering = true;

    // 清除之前的关闭定时器和监听器
    if (this._closeTimer) {
      clearTimeout(this._closeTimer);
      this._closeTimer = null;
    }
    if (this._closeHandler) {
      document.removeEventListener('click', this._closeHandler);
      this._closeHandler = null;
    }

    this.onSelectCallback = onSelect;

    // 先清除旧内容再渲染
    container.innerHTML = '';
    container.innerHTML = this.render();
    container.classList.add('active');

    // 使用事件委托，只绑定一次
    container.onclick = (e) => {
      const item = e.target.closest('.emoji-item');
      if (!item) return;
      e.preventDefault();
      e.stopPropagation();
      const emojiChar = item.dataset.emoji;
      const emojiName = item.title;
      console.log('[表情] 点击表情:', emojiName, emojiChar);
      if (onSelect) {
        onSelect(emojiName, emojiChar);
      }
      container.classList.remove('active');
      this._rendering = false;
    };

    // 点击外部关闭
    this._closeTimer = setTimeout(() => {
      this._closeHandler = (e) => {
        if (!container.contains(e.target) && !e.target.closest('#emojiBtn')) {
          container.classList.remove('active');
          document.removeEventListener('click', this._closeHandler);
          this._closeHandler = null;
          this._rendering = false;
        }
      };
      document.addEventListener('click', this._closeHandler);
    }, 100);
  },

  // 隐藏表情选择器
  hide(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
      container.classList.remove('active');
      container.innerHTML = '';
      this._rendering = false;
    }
  }
};
