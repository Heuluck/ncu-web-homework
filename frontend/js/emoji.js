/**
 * 表情选择器组件
 */
const EmojiPicker = {
  emojis: [],
  currentCategory: 'default',
  onSelectCallback: null,

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
    if (this.emojis.length === 0) {
      return '<div class="emoji-empty">暂无表情</div>';
    }

    let html = '<div class="emoji-grid">';
    this.emojis.forEach(emoji => {
      html += `
        <div class="emoji-item" data-emoji="${emoji.url}" title="${emoji.name}">
          <span style="font-size: 24px;">${emoji.url}</span>
        </div>
      `;
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

    this.onSelectCallback = onSelect;
    container.innerHTML = this.render();
    container.classList.add('active');

    // 绑定点击事件
    const items = container.querySelectorAll('.emoji-item');
    items.forEach((item, index) => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const emojiChar = item.dataset.emoji;
        const emojiName = item.title;
        console.log('[表情] 点击表情:', emojiName, emojiChar);
        if (onSelect) {
          onSelect(emojiName, emojiChar);  // name, char
        }
        container.classList.remove('active');
      });
    });

    // 点击外部关闭
    setTimeout(() => {
      const closeHandler = (e) => {
        if (!container.contains(e.target) && !e.target.closest('#emojiBtn')) {
          container.classList.remove('active');
          document.removeEventListener('click', closeHandler);
        }
      };
      document.addEventListener('click', closeHandler);
    }, 100);
  },

  // 隐藏表情选择器
  hide(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
      container.style.display = 'none';
    }
  }
};
