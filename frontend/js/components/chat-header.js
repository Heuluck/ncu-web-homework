/**
 * 聊天头部控制器 —— 单一入口，线程安全
 *
 * 设计原则：
 * 1. 所有对 #chatHeader / #chatName / #chatAvatar 等 DOM 的修改必须通过此控制器
 * 2. 使用会话令牌防止过期异步回调覆盖当前会话头部
 * 3. 存储当前上下文 { type, id, info }，任何更新操作先验证是否匹配
 *
 * 令牌机制：
 *   initSession() → 返回 token（同时递增内部令牌）
 *   renderPrivate(token) / renderGroup(token) → 仅当 token 匹配当前内部令牌时才渲染
 *   updateOnlineStatus(userId, status) → 仅当当前上下文是私聊且 userId 匹配才更新
 *   updateBlockStatus / updateGroupMemberCount 同理
 */
const ChatHeaderController = (() => {
  /** 内部令牌：每次 initSession 递增，async 渲染带回校验 */
  let _token = 0;
  /** 当前会话上下文 */
  let _ctx = null; // { type: 'private'|'group', id, info }

  // ==================== DOM 缓存（惰性初始化） ====================
  const _els = {};
  function el(key, id) {
    if (!_els[key]) _els[key] = document.getElementById(id);
    return _els[key];
  }

  function getHeader()      { return el('header', 'chatHeader'); }
  function getAvatar()      { return el('avatar', 'chatAvatar'); }
  function getStatus()      { return el('status', 'chatStatus'); }
  function getName()        { return el('name', 'chatName'); }
  function getStatusText()  { return el('statusText', 'chatStatusText'); }
  function getMoreBtn()     { return el('moreBtn', 'chatMoreBtn'); }
  function getMembersBtn()  { return el('membersBtn', 'groupMembersBtn'); }
  function getSettingsBtn() { return el('settingsBtn', 'groupSettingsBtn'); }
  function getVoiceBtn()    { return el('voiceBtn', 'voiceCallBtn'); }
  function getInputArea()   { return el('inputArea', 'chatInputArea'); }
  function getMessages()    { return el('messages', 'chatMessages'); }

  // ==================== 公开 API ====================

  /**
   * 初始化新会话上下文，返回令牌。
   * 调用方必须在打开会话时调用此方法，后续渲染操作携带返回的令牌。
   * @param {'private'|'group'} type
   * @param {number} id
   * @param {object} info
   * @returns {number} token
   */
  function initSession(type, id, info) {
    _ctx = { type, id, info };
    return ++_token;
  }

  /** 获取当前上下文（只读） */
  function getContext() {
    return _ctx;
  }

  /** 清除上下文 */
  function clearContext() {
    _ctx = null;
  }

  // ==================== 私聊渲染 ====================

  /**
   * 渲染私聊头部。必须在 initSession('private', ...) 之后调用，携带令牌。
   * @param {number} token
   */
  function renderPrivate(token) {
    if (token !== _token) return;
    if (!_ctx || _ctx.type !== 'private') return;
    const info = _ctx.info;
    if (!info) return;

    const header = getHeader();
    const avatar = getAvatar();
    const status = getStatus();
    const name = getName();
    const statusText = getStatusText();
    const moreBtn = getMoreBtn();

    // 隐藏群聊专属按钮
    const membersBtn = getMembersBtn();
    const settingsBtn = getSettingsBtn();
    const voiceBtn = getVoiceBtn();
    if (membersBtn) membersBtn.style.display = 'none';
    if (settingsBtn) settingsBtn.style.display = 'none';
    if (voiceBtn) voiceBtn.style.display = 'inline-flex';

    // 更多按钮恢复为私聊样式
    if (moreBtn) {
      moreBtn.innerHTML = '<i data-lucide="more-vertical"></i>';
      moreBtn.title = '更多';
      lucide.createIcons({ nodes: [moreBtn] });
    }

    if (header) header.style.display = '';

    // 头像
    if (avatar) {
      avatar.src = Utils.getAvatarUrl(info.avatar, `user-${info.friendId}`);
      avatar.alt = info.nickname;
      avatar.style.display = '';
      const letterEl = avatar.parentElement?.querySelector('.group-letter-avatar');
      if (letterEl) letterEl.remove();
    }

    // 名称 + 拉黑标记
    let nameHtml = Utils.escapeHtml(info.nickname || '');
    const bs = info.blockStatus;
    if (bs === 'blocked_by_me') nameHtml += ' <span style="color:var(--color-danger);font-size:12px;">(已拉黑)</span>';
    else if (bs === 'blocked_by_them') nameHtml += ' <span style="color:var(--color-warning);font-size:12px;">(被拉黑)</span>';
    else if (bs === 'both') nameHtml += ' <span style="color:var(--color-danger);font-size:12px;">(互相拉黑)</span>';
    if (name) name.innerHTML = nameHtml;

    // 在线状态
    if (status) {
      status.style.display = '';
      status.className = `status-indicator ${Utils.getStatusClass(info.onlineStatus)}`;
    }
    if (statusText) statusText.textContent = Utils.getStatusText(info.onlineStatus);
  }

  /**
   * 仅更新在线状态（无需完整重渲染）。由 WebSocket 状态推送触发。
   * @param {number} userId
   * @param {number} onlineStatus
   */
  function updateOnlineStatus(userId, onlineStatus) {
    if (!_ctx || _ctx.type !== 'private' || _ctx.id !== userId) return;
    // 同步上下文中的状态
    if (_ctx.info) _ctx.info.onlineStatus = onlineStatus;

    const status = getStatus();
    const statusText = getStatusText();
    if (status) {
      status.style.display = '';
      status.className = `status-indicator ${Utils.getStatusClass(onlineStatus)}`;
    }
    if (statusText) statusText.textContent = Utils.getStatusText(onlineStatus);
  }

  /**
   * 仅更新拉黑状态并重渲染。由 FriendManager 拉黑/解除拉黑操作触发。
   * @param {number} friendId
   * @param {string} blockStatus
   */
  function refreshBlockStatus(friendId, blockStatus) {
    if (!_ctx || _ctx.type !== 'private' || _ctx.id !== friendId) return;
    if (_ctx.info) _ctx.info.blockStatus = blockStatus;
    // 使用当前令牌重渲染（因为上下文未变，令牌仍有效）
    renderPrivate(_token);
  }

  // ==================== 群聊渲染 ====================

  /**
   * 渲染群聊头部。必须在 initSession('group', ...) 之后调用，携带令牌。
   * @param {number} token
   */
  function renderGroup(token) {
    if (token !== _token) return;
    if (!_ctx || _ctx.type !== 'group') return;
    const info = _ctx.info;
    if (!info) return;

    const header = getHeader();
    const avatar = getAvatar();
    const status = getStatus();
    const name = getName();
    const statusText = getStatusText();
    const moreBtn = getMoreBtn();

    // 显示群聊专属按钮
    const membersBtn = getMembersBtn();
    const settingsBtn = getSettingsBtn();
    const voiceBtn = getVoiceBtn();
    if (membersBtn) membersBtn.style.display = 'inline-flex';
    if (settingsBtn) settingsBtn.style.display = 'inline-flex';
    if (voiceBtn) voiceBtn.style.display = 'none';

    // 更多按钮改为机器人图标
    if (moreBtn) {
      moreBtn.innerHTML = '<i data-lucide="bot"></i>';
      moreBtn.title = 'AI 机器人';
      lucide.createIcons({ nodes: [moreBtn] });
    }

    if (header) header.style.display = '';

    // 头像：有图用图，无图用群名首字
    if (avatar) {
      avatar.alt = info.name || '群聊';
      if (info.avatar) {
        avatar.src = info.avatar;
        avatar.style.display = '';
        const oldLetter = avatar.parentElement?.querySelector('.group-letter-avatar');
        if (oldLetter) oldLetter.remove();
      } else {
        avatar.style.display = 'none';
        const wrapper = avatar.parentElement;
        let letterEl = wrapper?.querySelector('.group-letter-avatar');
        if (!letterEl && wrapper) {
          letterEl = document.createElement('div');
          letterEl.className = 'avatar avatar-md group-letter-avatar';
          letterEl.style.cssText = 'background:var(--color-primary);color:#fff;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:600;';
          wrapper.insertBefore(letterEl, avatar);
        }
        if (letterEl) letterEl.textContent = (info.name || '?')[0];
      }
    }

    if (name) name.textContent = info.name;
    if (status) status.style.display = 'none';
    if (statusText) statusText.textContent = `${info.memberCount || 0} 人`;
  }

  /**
   * 更新群聊成员数。由群事件（成员加入/离开）触发。
   * @param {number} groupId
   * @param {number} memberCount
   */
  function updateGroupMemberCount(groupId, memberCount) {
    if (!_ctx || _ctx.type !== 'group' || _ctx.id !== groupId) return;
    const statusText = getStatusText();
    if (statusText) statusText.textContent = `${memberCount} 人`;
  }

  // ==================== 隐藏 / 重置 ====================

  /** 隐藏头部和输入区（保留令牌验证） */
  function hide(token) {
    if (token !== _token) return;
    const header = getHeader();
    const inputArea = getInputArea();
    if (header) header.style.display = 'none';
    if (inputArea) inputArea.style.display = 'none';
  }

  /** 显示输入区 */
  function showInputArea() {
    const inputArea = getInputArea();
    if (inputArea) inputArea.style.display = '';
  }

  /** 重置为空白状态（无活跃聊天）。用于删除好友、解散群聊等显式操作，无需令牌验证。 */
  function showEmptyState(messagesHtml) {
    _ctx = null;
    _token++; // 使任何进行中的 async 渲染失效
    const header = getHeader();
    const inputArea = getInputArea();
    const messages = getMessages();
    if (header) header.style.display = 'none';
    if (inputArea) inputArea.style.display = 'none';
    if (messages) messages.innerHTML = messagesHtml;
    lucide.createIcons();
  }

  // ==================== 导出 ====================
  return {
    initSession,
    getContext,
    clearContext,
    renderPrivate,
    renderGroup,
    updateOnlineStatus,
    refreshBlockStatus,
    updateGroupMemberCount,
    hide,
    showInputArea,
    showEmptyState
  };
})();
