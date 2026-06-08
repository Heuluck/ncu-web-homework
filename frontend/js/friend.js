/**
 * 好友管理器
 * 负责好友列表、搜索、申请、分组、拉黑、右键菜单等功能
 */
const FriendManager = {
    groups: [],
    currentContextFriendId: null,
    currentContextFriendshipId: null,
    pendingCount: 0,

    // ==================== 好友列表 ====================

    async loadFriends() {
        const res = await API.get('/api/friend/list');
        if (res && res.code === 200) {
            this.groups = res.data || [];
            this.renderFriendList();
        }
        this.checkPendingRequests();
    },

    async checkPendingRequests() {
        const res = await API.get('/api/friend/requests/received');
        this.pendingCount = (res && res.code === 200 && res.data) ? res.data.length : 0;
        this._updateRequestBadge();
    },

    _updateRequestBadge() {
        // 导航栏上的 badge
        const navBadge = document.getElementById('friendRequestBadge');
        if (navBadge) {
            if (this.pendingCount > 0) {
                navBadge.textContent = this.pendingCount;
                navBadge.style.display = '';
            } else {
                navBadge.style.display = 'none';
            }
        }
        // 按钮上的 badge（重新渲染列表时更新）
        const btnBadge = document.getElementById('requestBtnBadge');
        if (btnBadge) {
            if (this.pendingCount > 0) {
                btnBadge.textContent = this.pendingCount;
                btnBadge.style.display = '';
            } else {
                btnBadge.style.display = 'none';
            }
        }
    },

    renderFriendList() {
        const container = document.getElementById('friendList');
        if (!container) return;

        if (!this.groups || this.groups.length === 0) {
            container.innerHTML = `
                <div style="padding: 12px; display: flex; gap: 8px;">
                    <button class="btn btn-primary btn-sm" onclick="FriendManager.openSearchModal()" style="flex: 1;">
                        <i data-lucide="user-plus" style="width: 14px; height: 14px;"></i> 添加好友
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="FriendManager.openRequestsModal()" style="flex: 1;">
                        <i data-lucide="mail" style="width: 14px; height: 14px;"></i> 好友申请<span class="badge" id="requestBtnBadge" style="display:none;margin-left:4px;"></span>
                    </button>
                </div>
                <div class="empty-state" style="padding: 40px 20px; text-align: center;">
                    <div class="empty-state-icon">
                        <i data-lucide="users" style="width: 40px; height: 40px;"></i>
                    </div>
                    <div class="empty-state-title" style="font-size: 14px;">暂无好友</div>
                    <div class="empty-state-text" style="font-size: 12px;">搜索用户来添加好友吧</div>
                </div>`;
            lucide.createIcons();
            return;
        }

        let html = '';
        // 管理按钮
        html += `
            <div style="padding: 12px; display: flex; gap: 8px;">
                <button class="btn btn-primary btn-sm" onclick="FriendManager.openSearchModal()" style="flex: 1;">
                    <i data-lucide="user-plus" style="width: 14px; height: 14px;"></i> 添加好友
                </button>
                <button class="btn btn-secondary btn-sm" onclick="FriendManager.openRequestsModal()" style="flex: 1;">
                    <i data-lucide="mail" style="width: 14px; height: 14px;"></i> 好友申请<span class="badge" id="requestBtnBadge" style="display:none;margin-left:4px;"></span>
                </button>
                <button class="btn btn-secondary btn-sm" onclick="FriendManager.openGroupManageModal()">
                    <i data-lucide="folder" style="width: 14px; height: 14px;"></i>
                </button>
            </div>`;

        const blockedFriends = [];

        for (const group of this.groups) {
            if (!group.friends || group.friends.length === 0) continue;
            const normalFriends = group.friends.filter(f => f.blockStatus === 'none');
            const blocked = group.friends.filter(f => f.blockStatus !== 'none');
            blockedFriends.push(...blocked);

            if (normalFriends.length > 0) {
                html += `<div class="list-group-title">${Utils.escapeHtml(group.name)}</div>`;
                for (const friend of normalFriends) {
                    html += this._renderFriendItem(friend);
                }
            }
        }

        // 黑名单独立区域
        if (blockedFriends.length > 0) {
            html += `<div class="list-group-title" style="color:var(--color-danger);">黑名单 (${blockedFriends.length})</div>`;
            for (const friend of blockedFriends) {
                html += this._renderFriendItem(friend);
            }
        }

        container.innerHTML = html;

        // 绑定点击事件
        container.querySelectorAll('.list-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const friendId = parseInt(item.dataset.friendId);
                const friendshipId = parseInt(item.dataset.friendshipId);
                // 黑名单也可点击进入聊天
                let fallbackInfo = null;
                for (const group of (this.groups || [])) {
                    const friend = (group.friends || []).find(f => f.friendId === friendId);
                    if (friend) {
                        fallbackInfo = {
                            friendId: friend.friendId,
                            nickname: friend.nickname,
                            avatar: friend.avatar,
                            onlineStatus: friend.onlineStatus,
                            blockStatus: friend.blockStatus
                        };
                        break;
                    }
                }
                GroupManager.currentGroupId = null;
                GroupManager.currentGroupInfo = null;
                ChatManager.openChat(friendId, fallbackInfo);
                document.querySelector('.nav-item[data-tab="recent"]')?.click();
            });
        });

        lucide.createIcons();
    },

    // ==================== 搜索用户弹窗 ====================

    openSearchModal() {
        this._closeAnyModal();
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'searchModal';
        overlay.innerHTML = `
            <div class="modal-container" style="max-width: 480px;">
                <div class="modal-header">
                    <h3>搜索用户</h3>
                    <button class="btn-icon" onclick="FriendManager.closeSearchModal()">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <input type="text" id="searchKeyword" class="input-control" placeholder="输入用户名或昵称搜索..." onkeydown="if(event.key==='Enter'&&!event.isComposing)FriendManager.doSearch()">
                    </div>
                    <div id="searchResults" style="margin-top: 12px;"></div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="FriendManager.closeSearchModal()">关闭</button>
                    <button class="btn btn-primary" onclick="FriendManager.doSearch()">搜索</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', (e) => { if (e.target === overlay) this.closeSearchModal(); });
        lucide.createIcons();
        setTimeout(() => document.getElementById('searchKeyword')?.focus(), 100);
    },

    closeSearchModal() {
        document.getElementById('searchModal')?.remove();
    },

    async doSearch() {
        const keyword = document.getElementById('searchKeyword')?.value?.trim();
        if (!keyword) {
            Utils.showToast('请输入搜索关键词', 'warning');
            return;
        }

        const res = await API.post('/api/friend/search', { keyword });
        const resultsEl = document.getElementById('searchResults');
        if (!resultsEl) return;

        if (!res || res.code !== 200 || !res.data || res.data.length === 0) {
            resultsEl.innerHTML = '<div class="empty-state" style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 13px;">未找到相关用户</div>';
            return;
        }

        let html = '';
        for (const user of res.data) {
            const avatarSrc = Utils.getAvatarUrl(user.avatar, `user-${user.id}`);
            const statusText = Utils.getStatusText(user.onlineStatus);
            html += `
            <div class="friend-item">
                <img src="${avatarSrc}" class="avatar avatar-md">
                <div style="flex: 1;">
                    <div style="font-size: 14px; font-weight: 500;">${Utils.escapeHtml(user.nickname)}</div>
                    <div style="font-size: 12px; color: var(--text-muted);">@${Utils.escapeHtml(user.username)} · ${statusText}</div>
                    ${user.signature ? `<div style="font-size: 12px; color: var(--text-muted);">${Utils.escapeHtml(user.signature)}</div>` : ''}
                </div>
                <div>
                    ${user.isFriend
                        ? '<span style="font-size: 12px; color: var(--text-muted);">已是好友</span>'
                        : (user.hasPendingRequest
                            ? '<span style="font-size: 12px; color: var(--color-warning);">已申请</span>'
                            : `<button class="btn btn-primary btn-sm" onclick="FriendManager.showRequestForm(${user.id}, '${Utils.escapeHtml(user.nickname)}')">
                                <i data-lucide="user-plus" style="width: 12px; height: 12px;"></i> 添加
                               </button>`)
                    }
                </div>
            </div>`;
        }
        resultsEl.innerHTML = html;
        lucide.createIcons();
    },

    showRequestForm(friendId, friendName) {
        const resultsEl = document.getElementById('searchResults');
        if (!resultsEl) return;
        // 在结果列表中展开一个验证信息输入框
        resultsEl.innerHTML = `
            <div style="padding: 12px; background: var(--bg-surface); border-radius: var(--radius-lg);">
                <div style="font-size: 14px; margin-bottom: 8px;">发送好友申请给 <strong>${friendName}</strong></div>
                <div class="form-group">
                    <label class="form-label">分组</label>
                    <select id="requestGroupSelect" class="input-control">
                        ${(this.groups || []).map(g => `<option value="${g.groupId}">${Utils.escapeHtml(g.name)}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">验证信息（可选）</label>
                    <input type="text" id="requestMessage" class="input-control" placeholder="你好，我是...">
                </div>
                <div style="display: flex; gap: 8px; justify-content: flex-end;">
                    <button class="btn btn-secondary btn-sm" onclick="FriendManager.doSearch()">取消</button>
                    <button class="btn btn-primary btn-sm" onclick="FriendManager.sendRequest(${friendId})">发送申请</button>
                </div>
            </div>`;
    },

    async sendRequest(friendId) {
        const groupId = document.getElementById('requestGroupSelect')?.value;
        const message = document.getElementById('requestMessage')?.value?.trim();
        const res = await API.post('/api/friend/request', {
            friendId: friendId,
            groupId: groupId ? parseInt(groupId) : null,
            verificationMessage: message || null
        });
        if (res && res.code === 200) {
            Utils.showToast('好友申请已发送', 'success');
            this.closeSearchModal();
        } else {
            Utils.showToast(res?.message || '发送失败', 'error');
        }
    },

    // ==================== 好友申请弹窗 ====================

    async openRequestsModal() {
        this._closeAnyModal();
        const [receivedRes, sentRes] = await Promise.all([
            API.get('/api/friend/requests/received'),
            API.get('/api/friend/requests/sent')
        ]);

        const received = (receivedRes && receivedRes.code === 200) ? receivedRes.data || [] : [];
        const sent = (sentRes && sentRes.code === 200) ? sentRes.data || [] : [];

        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'requestsModal';
        overlay.innerHTML = `
            <div class="modal-container" style="max-width: 520px; max-height: 80vh;">
                <div class="modal-header">
                    <h3>好友申请</h3>
                    <button class="btn-icon" onclick="FriendManager.closeRequestsModal()">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div style="display: flex; border-bottom: 1px solid var(--border-subtle);">
                    <div class="request-tab active" data-rtab="received" onclick="FriendManager.switchRequestTab('received')"
                         style="flex:1;text-align:center;padding:12px;cursor:pointer;font-size:14px;font-weight:500;border-bottom:2px solid var(--color-primary);color:var(--color-primary);">
                        收到的申请 ${received.length > 0 ? `<span class="badge" style="margin-left:4px;">${received.length}</span>` : ''}
                    </div>
                    <div class="request-tab" data-rtab="sent" onclick="FriendManager.switchRequestTab('sent')"
                         style="flex:1;text-align:center;padding:12px;cursor:pointer;font-size:14px;font-weight:500;border-bottom:2px solid transparent;color:var(--text-muted);">
                        发出的申请
                    </div>
                </div>
                <div class="modal-body" id="requestsModalBody">
                    ${this._renderRequestList('received', received)}
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', (e) => { if (e.target === overlay) this.closeRequestsModal(); });
        // 存储数据以便 tab 切换
        overlay._received = received;
        overlay._sent = sent;
        lucide.createIcons();
    },

    closeRequestsModal() {
        document.getElementById('requestsModal')?.remove();
    },

    switchRequestTab(tab) {
        const overlay = document.getElementById('requestsModal');
        if (!overlay) return;
        document.querySelectorAll('.request-tab').forEach(t => {
            t.classList.remove('active');
            t.style.borderBottom = '2px solid transparent';
            t.style.color = 'var(--text-muted)';
        });
        const activeTab = document.querySelector(`.request-tab[data-rtab="${tab}"]`);
        if (activeTab) {
            activeTab.classList.add('active');
            activeTab.style.borderBottom = '2px solid var(--color-primary)';
            activeTab.style.color = 'var(--color-primary)';
        }
        const data = tab === 'received' ? overlay._received : overlay._sent;
        document.getElementById('requestsModalBody').innerHTML = this._renderRequestList(tab, data);
        lucide.createIcons();
    },

    _renderRequestList(type, items) {
        if (!items || items.length === 0) {
            return `<div class="empty-state" style="padding: 30px; text-align: center; color: var(--text-muted); font-size: 13px;">
                ${type === 'received' ? '暂无收到的好友申请' : '暂无发出的好友申请'}
            </div>`;
        }
        let html = '';
        for (const item of items) {
            const avatarSrc = Utils.getAvatarUrl(item.avatar, `user-${item.userId}`);
            const timeText = Utils.formatTime(item.createTime);
            html += `
            <div class="friend-item" style="padding: 12px; display: flex; gap: 12px; align-items: center;">
                <img src="${avatarSrc}" class="avatar avatar-md">
                <div style="flex: 1; min-width: 0;">
                    <div style="font-size: 14px; font-weight: 500;">${Utils.escapeHtml(item.nickname)}</div>
                    <div style="font-size: 12px; color: var(--text-muted);">@${Utils.escapeHtml(item.username)} · ${timeText}</div>
                    ${item.verificationMessage ? `<div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">验证信息：${Utils.escapeHtml(item.verificationMessage)}</div>` : ''}
                </div>
                ${type === 'received' ? `
                <div style="display: flex; gap: 6px; flex-shrink: 0;">
                    <button class="btn btn-primary btn-sm" onclick="FriendManager.acceptRequest(${item.friendshipId})">接受</button>
                    <button class="btn btn-secondary btn-sm" onclick="FriendManager.rejectRequest(${item.friendshipId})">拒绝</button>
                </div>` : `
                <span style="font-size: 12px; color: var(--text-muted); flex-shrink: 0;">等待验证</span>`}
            </div>`;
        }
        return html;
    },

    async acceptRequest(friendshipId) {
        const res = await API.put(`/api/friend/request/${friendshipId}/accept`, {});
        if (res && res.code === 200) {
            Utils.showToast('已接受好友申请', 'success');
            this.pendingCount = Math.max(0, this.pendingCount - 1);
            this._updateRequestBadge();
            this.closeRequestsModal();
            this.loadFriends();
            ConversationManager.loadConversations();
        } else {
            Utils.showToast(res?.message || '操作失败', 'error');
        }
    },

    async rejectRequest(friendshipId) {
        const res = await API.put(`/api/friend/request/${friendshipId}/reject`, {});
        if (res && res.code === 200) {
            Utils.showToast('已拒绝好友申请', 'success');
            this.pendingCount = Math.max(0, this.pendingCount - 1);
            this._updateRequestBadge();
            this.closeRequestsModal();
        } else {
            Utils.showToast(res?.message || '操作失败', 'error');
        }
    },

    // ==================== 右键菜单 ====================

    showContextMenu(friendshipId, friendId, friendName, blockStatus, event) {
        event.preventDefault();
        this._closeContextMenu();
        this.currentContextFriendId = friendId;
        this.currentContextFriendshipId = friendshipId;

        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.id = 'contextMenu';
        menu.style.cssText = `position:fixed;left:${event.clientX}px;top:${event.clientY}px;z-index:2000;`;

        const canUnblock = blockStatus === 'blocked_by_me' || blockStatus === 'both';
        const canBlock = blockStatus === 'none' || blockStatus === 'blocked_by_them';

        menu.innerHTML = `
            <div style="background:var(--bg-app);border:1px solid var(--border-subtle);border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);min-width:180px;padding:4px 0;">
                <div class="context-menu-item" onclick="FriendManager._ctxSendMessage(); FriendManager._closeContextMenu();">
                    <i data-lucide="message-circle" style="width:16px;height:16px;"></i> 发送消息
                </div>
                <div class="context-menu-item" onclick="FriendManager._ctxViewProfile(); FriendManager._closeContextMenu();">
                    <i data-lucide="user" style="width:16px;height:16px;"></i> 查看资料
                </div>
                <div class="context-divider"></div>
                <div class="context-menu-item" onclick="FriendManager._ctxMoveFriend(); FriendManager._closeContextMenu();">
                    <i data-lucide="folder-input" style="width:16px;height:16px;"></i> 移动到分组
                </div>
                ${canUnblock ? `
                <div class="context-menu-item" onclick="FriendManager._ctxUnblock(); FriendManager._closeContextMenu();" style="color:var(--color-primary);">
                    <i data-lucide="shield-off" style="width:16px;height:16px;"></i> 取消拉黑
                </div>` : ''}
                ${canBlock ? `
                <div class="context-divider"></div>
                <div class="context-menu-item" onclick="FriendManager._ctxBlock(); FriendManager._closeContextMenu();" style="color:var(--color-danger);">
                    <i data-lucide="shield" style="width:16px;height:16px;"></i> 拉黑
                </div>` : ''}
                <div class="context-menu-item" onclick="FriendManager._ctxDelete(); FriendManager._closeContextMenu();" style="color:var(--color-danger);">
                    <i data-lucide="user-x" style="width:16px;height:16px;"></i> 删除好友
                </div>
            </div>`;

        document.body.appendChild(menu);
        lucide.createIcons();

        // 自动调整菜单位置防止溢出
        const rect = menu.getBoundingClientRect();
        if (rect.right > window.innerWidth) menu.style.left = (window.innerWidth - rect.width - 8) + 'px';
        if (rect.bottom > window.innerHeight) menu.style.top = (window.innerHeight - rect.height - 8) + 'px';

        // 点击其他地方关闭
        setTimeout(() => {
            document.addEventListener('click', this._closeContextMenu, { once: true });
            document.addEventListener('contextmenu', this._closeContextMenu, { once: true });
        }, 0);
    },

    _closeContextMenu() {
        document.getElementById('contextMenu')?.remove();
    },

    _ctxSendMessage() {
        const friendId = this.currentContextFriendId || this._profileFriendId;
        if (friendId) {
            // 关闭所有模态框
            this._closeAnyModal();
            // 同时关闭群成员面板
            if (typeof GroupMemberPanel !== 'undefined') {
                GroupMemberPanel.close();
            }
            let fallbackInfo = null;
            for (const group of (this.groups || [])) {
                const friend = (group.friends || []).find(f => f.friendId === friendId);
                if (friend) {
                    fallbackInfo = { friendId: friend.friendId, nickname: friend.nickname, avatar: friend.avatar, onlineStatus: friend.onlineStatus };
                    break;
                }
            }
            // 如果不在好友列表中，使用资料弹窗缓存的信息
            if (!fallbackInfo && this._profileUserInfo && this._profileUserInfo.id === friendId) {
                fallbackInfo = {
                    friendId: this._profileUserInfo.id,
                    nickname: this._profileUserInfo.nickname,
                    avatar: this._profileUserInfo.avatar,
                    onlineStatus: this._profileUserInfo.status || 0
                };
            }
            GroupManager.currentGroupId = null;
            GroupManager.currentGroupInfo = null;
            ChatManager.openChat(friendId, fallbackInfo);
            document.querySelector('.nav-item[data-tab="recent"]')?.click();
        }
    },

    _ctxViewProfile() {
        if (this.currentContextFriendId) {
            this.openProfileModal(this.currentContextFriendId);
        }
    },

    _ctxMoveFriend() {
        if (!this.currentContextFriendshipId) return;
        this.openMoveFriendModal(this.currentContextFriendshipId);
    },

    async _ctxBlock() {
        if (!this.currentContextFriendshipId) return;
        const res = await API.put(`/api/friend/${this.currentContextFriendshipId}/block`, {});
        if (res && res.code === 200) {
            Utils.showToast('已拉黑', 'success');
            for (const group of this.groups) {
                const f = (group.friends || []).find(x => x.friendshipId === this.currentContextFriendshipId);
                if (f) {
                    f.blockStatus = f.blockStatus === 'blocked_by_them' ? 'both' : 'blocked_by_me';
                    break;
                }
            }
            // 同步更新聊天头+输入框状态
            if (ChatManager.currentFriendInfo) {
                for (const group of this.groups) {
                    const f = (group.friends || []).find(x => x.friendId === ChatManager.currentFriendInfo.friendId);
                    if (f) { ChatManager.currentFriendInfo.blockStatus = f.blockStatus; break; }
                }
                ChatHeaderController.refreshBlockStatus(ChatManager.currentFriendId, ChatManager.currentFriendInfo.blockStatus);
                ChatManager._updateInputState();
            }
            this.loadFriends();
            ConversationManager.loadConversations();
        } else {
            Utils.showToast(res?.message || '操作失败', 'error');
        }
    },

    async _ctxUnblock() {
        if (!this.currentContextFriendshipId) return;
        const res = await API.put(`/api/friend/${this.currentContextFriendshipId}/unblock`, {});
        if (res && res.code === 200) {
            Utils.showToast('已取消拉黑', 'success');
            for (const group of this.groups) {
                const f = (group.friends || []).find(x => x.friendshipId === this.currentContextFriendshipId);
                if (f) {
                    f.blockStatus = f.blockStatus === 'both' ? 'blocked_by_them' : 'none';
                    break;
                }
            }
            // 同步更新聊天头+输入框状态
            if (ChatManager.currentFriendInfo) {
                for (const group of this.groups) {
                    const f = (group.friends || []).find(x => x.friendId === ChatManager.currentFriendInfo.friendId);
                    if (f) { ChatManager.currentFriendInfo.blockStatus = f.blockStatus; break; }
                }
                ChatHeaderController.refreshBlockStatus(ChatManager.currentFriendId, ChatManager.currentFriendInfo.blockStatus);
                ChatManager._updateInputState();
            }
            this.loadFriends();
            ConversationManager.loadConversations();
        } else {
            Utils.showToast(res?.message || '操作失败', 'error');
        }
    },

    async _ctxDelete() {
        if (!this.currentContextFriendshipId) return;
        if (!await Utils.showConfirm('确定要删除该好友吗？', { title: '删除好友', confirmText: '删除', danger: true })) return;
        const res = await API.delete(`/api/friend/${this.currentContextFriendshipId}`);
        if (res && res.code === 200) {
            Utils.showToast('已删除好友', 'success');
            ChatManager.currentFriendId = null;
            ChatManager.currentFriendInfo = null;
            ChatHeaderController.showEmptyState(
                '<div class="empty-state"><div class="empty-state-icon"><i data-lucide="message-square" style="width:64px;height:64px;"></i></div><div class="empty-state-title">选择一个会话开始聊天</div></div>'
            );
            this.loadFriends();
            ConversationManager.loadConversations();
        } else {
            Utils.showToast(res?.message || '删除失败', 'error');
        }
    },

    // ==================== 移动好友弹窗 ====================

    openMoveFriendModal(friendshipId) {
        this._closeAnyModal();
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'moveFriendModal';
        let groupOptions = (this.groups || []).map(g =>
            `<option value="${g.groupId}">${Utils.escapeHtml(g.name)}</option>`
        ).join('');

        overlay.innerHTML = `
            <div class="modal-container" style="max-width: 400px;">
                <div class="modal-header">
                    <h3>移动到分组</h3>
                    <button class="btn-icon" onclick="FriendManager.closeMoveFriendModal()">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label class="form-label">选择分组</label>
                        <select id="moveGroupSelect" class="input-control">${groupOptions}</select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="FriendManager.closeMoveFriendModal()">取消</button>
                    <button class="btn btn-primary" onclick="FriendManager.doMoveFriend(${friendshipId})">确定</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', (e) => { if (e.target === overlay) this.closeMoveFriendModal(); });
        lucide.createIcons();
    },

    closeMoveFriendModal() {
        document.getElementById('moveFriendModal')?.remove();
    },

    async doMoveFriend(friendshipId) {
        const groupId = parseInt(document.getElementById('moveGroupSelect')?.value);
        if (!groupId) return;
        const res = await API.put(`/api/friend/${friendshipId}/move-group`, { groupId });
        if (res && res.code === 200) {
            Utils.showToast('移动成功', 'success');
            this.closeMoveFriendModal();
            this.loadFriends();
        } else {
            Utils.showToast(res?.message || '操作失败', 'error');
        }
    },

    // ==================== 资料弹窗 ====================

    async openProfileModal(friendId) {
        this._closeAnyModal();
        this._profileFriendId = friendId;
        // 使用搜索接口获取单个用户信息
        const res = await API.get(`/api/user/info/${friendId}`);
        if (!res || res.code !== 200 || !res.data) {
            Utils.showToast('获取用户信息失败', 'error');
            this._profileFriendId = null;
            return;
        }
        const user = res.data;
        // 缓存用户信息，供 _ctxSendMessage 使用
        this._profileUserInfo = user;
        const avatarSrc = Utils.getAvatarUrl(user.avatar, `user-${user.id}`);
        const statusText = Utils.getStatusText(user.status);
        const statusClass = Utils.getStatusClass(user.status);

        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'profileModal';
        overlay.innerHTML = `
            <div class="modal-container" style="max-width: 380px;">
                <div class="modal-header">
                    <h3>好友资料</h3>
                    <button class="btn-icon" onclick="FriendManager.closeProfileModal()">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div class="modal-body" style="text-align: center;">
                    <div class="avatar-wrapper" style="display: inline-block; margin-bottom: 12px; cursor: pointer; position: relative;" onclick="GroupManager.showImagePreview('${avatarSrc.replace(/'/g, "\\'")}')" title="查看大图">
                        <img src="${avatarSrc}" class="avatar avatar-lg" style="width: 80px; height: 80px;">
                        <span class="status-indicator ${statusClass}" style="position: absolute; bottom: 2px; right: 2px;"></span>
                    </div>
                    <div style="font-size: 18px; font-weight: 600; margin-bottom: 4px;">${Utils.escapeHtml(user.nickname)}</div>
                    <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 8px;">@${Utils.escapeHtml(user.username)} · ${statusText}</div>
                    ${user.signature ? `<div style="font-size: 13px; color: var(--text-muted); padding: 8px; background: var(--bg-surface); border-radius: var(--radius-md);">${Utils.escapeHtml(user.signature)}</div>` : ''}
                </div>
                ${user.id !== Auth.getUserId() ? `
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="FriendManager._ctxSendMessage();">
                        <i data-lucide="message-circle" style="width: 14px; height: 14px;"></i> 发消息
                    </button>
                </div>` : ''}
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', (e) => { if (e.target === overlay) this.closeProfileModal(); });
        lucide.createIcons();
    },

    closeProfileModal() {
        document.getElementById('profileModal')?.remove();
        this._profileFriendId = null;
    },

    // ==================== 分组管理弹窗 ====================

    openGroupManageModal() {
        this._closeAnyModal();
        const overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'groupManageModal';
        overlay.innerHTML = `
            <div class="modal-container" style="max-width: 420px;">
                <div class="modal-header">
                    <h3>管理分组</h3>
                    <button class="btn-icon" onclick="FriendManager.closeGroupManageModal()">
                        <i data-lucide="x"></i>
                    </button>
                </div>
                <div class="modal-body" id="groupManageBody">
                    ${this._renderGroupManageList()}
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary btn-sm" onclick="FriendManager.showCreateGroupForm()">
                        <i data-lucide="plus" style="width: 14px; height: 14px;"></i> 新建分组
                    </button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        overlay.addEventListener('click', (e) => { if (e.target === overlay) this.closeGroupManageModal(); });
        lucide.createIcons();
    },

    closeGroupManageModal() {
        document.getElementById('groupManageModal')?.remove();
    },

    _renderGroupManageList() {
        if (!this.groups || this.groups.length === 0) {
            return '<div class="empty-state" style="padding:20px;text-align:center;color:var(--text-muted);">暂无分组</div>';
        }
        let html = '';
        for (const group of this.groups) {
            const friendCount = group.friends ? group.friends.length : 0;
            html += `
            <div style="display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border-subtle);">
                <div style="flex: 1;">
                    <span style="font-size: 14px;">${Utils.escapeHtml(group.name)}</span>
                    <span style="font-size: 12px; color: var(--text-muted); margin-left: 8px;">${friendCount}位好友</span>
                    ${group.isDefault ? '<span style="font-size: 11px; color: var(--color-primary); margin-left: 4px;">默认</span>' : ''}
                </div>
                ${group.groupId !== 0 ? `
                <button class="btn-icon" onclick="FriendManager.showRenameGroupForm(${group.groupId}, '${Utils.escapeHtml(group.name)}')" title="重命名">
                    <i data-lucide="pencil" style="width: 14px; height: 14px;"></i>
                </button>` : ''}
                ${!group.isDefault && group.groupId !== 0 ? `
                <button class="btn-icon" onclick="FriendManager.deleteGroupConfirm(${group.groupId}, '${Utils.escapeHtml(group.name)}')" title="删除">
                    <i data-lucide="trash-2" style="width: 14px; height: 14px; color: var(--color-danger);"></i>
                </button>` : ''}
            </div>`;
        }
        return html;
    },

    refreshGroupManageBody() {
        const body = document.getElementById('groupManageBody');
        if (body) {
            body.innerHTML = this._renderGroupManageList();
            lucide.createIcons();
        }
    },

    showCreateGroupForm() {
        const body = document.getElementById('groupManageBody');
        if (!body) return;
        body.innerHTML = `
            <div class="form-group">
                <label class="form-label">分组名称</label>
                <input type="text" id="newGroupName" class="input-control" placeholder="输入分组名称" maxlength="50">
            </div>
            <div style="display: flex; gap: 8px; justify-content: flex-end;">
                <button class="btn btn-secondary btn-sm" onclick="FriendManager.refreshGroupManageBody()">取消</button>
                <button class="btn btn-primary btn-sm" onclick="FriendManager.doCreateGroup()">创建</button>
            </div>`;
        setTimeout(() => document.getElementById('newGroupName')?.focus(), 100);
    },

    async doCreateGroup() {
        const name = document.getElementById('newGroupName')?.value?.trim();
        if (!name) {
            Utils.showToast('请输入分组名称', 'warning');
            return;
        }
        const res = await API.post('/api/friend/group', { name });
        if (res && res.code === 200) {
            Utils.showToast('分组已创建', 'success');
            await this.loadFriends();
            this.refreshGroupManageBody();
        } else {
            Utils.showToast(res?.message || '创建失败', 'error');
        }
    },

    showRenameGroupForm(groupId, currentName) {
        const body = document.getElementById('groupManageBody');
        if (!body) return;
        body.innerHTML = `
            <div class="form-group">
                <label class="form-label">重命名分组</label>
                <input type="text" id="renameGroupName" class="input-control" value="${currentName}" maxlength="50">
            </div>
            <div style="display: flex; gap: 8px; justify-content: flex-end;">
                <button class="btn btn-secondary btn-sm" onclick="FriendManager.refreshGroupManageBody()">取消</button>
                <button class="btn btn-primary btn-sm" onclick="FriendManager.doRenameGroup(${groupId})">保存</button>
            </div>`;
        setTimeout(() => document.getElementById('renameGroupName')?.select(), 100);
    },

    async doRenameGroup(groupId) {
        const name = document.getElementById('renameGroupName')?.value?.trim();
        if (!name) {
            Utils.showToast('请输入分组名称', 'warning');
            return;
        }
        const res = await API.put(`/api/friend/group/${groupId}`, { name });
        if (res && res.code === 200) {
            Utils.showToast('分组已重命名', 'success');
            await this.loadFriends();
            this.refreshGroupManageBody();
        } else {
            Utils.showToast(res?.message || '重命名失败', 'error');
        }
    },

    async deleteGroupConfirm(groupId, groupName) {
        if (!await Utils.showConfirm(`确定要删除分组「${groupName}」吗？该分组下的好友将移入默认分组。`, { title: '删除分组', confirmText: '删除', danger: true })) return;
        const res = await API.delete(`/api/friend/group/${groupId}`);
        if (res && res.code === 200) {
            Utils.showToast('分组已删除', 'success');
            await this.loadFriends();
            this.refreshGroupManageBody();
        } else {
            Utils.showToast(res?.message || '删除失败', 'error');
        }
    },

    // ==================== 在线状态同步 ====================

    /** WebSocket 推送状态变更时更新好友列表中对应好友的状态 */
    updateOnlineStatus(userId, status) {
        if (!this.groups) return;
        for (const group of this.groups) {
            const friend = (group.friends || []).find(f => f.friendId === userId);
            if (friend) {
                friend.onlineStatus = status;
                this.renderFriendList();
                return;
            }
        }
    },

    // ==================== 工具 ====================

    _renderFriendItem(friend) {
        const isBlocked = friend.blockStatus !== 'none';
        const statusClass = Utils.getStatusClass(friend.onlineStatus);
        const avatarSrc = Utils.getAvatarUrl(friend.avatar, `user-${friend.friendId}`);
        let blockLabel = '';
        if (friend.blockStatus === 'blocked_by_me') blockLabel = '<span style="color:var(--color-danger);font-size:11px;">已拉黑</span>';
        else if (friend.blockStatus === 'blocked_by_them') blockLabel = '<span style="color:var(--color-warning);font-size:11px;">被拉黑</span>';
        else if (friend.blockStatus === 'both') blockLabel = '<span style="color:var(--color-danger);font-size:11px;">互相拉黑</span>';
        return `
        <div class="list-item ${isBlocked ? 'blocked' : ''}"
             data-friendship-id="${friend.friendshipId}"
             data-friend-id="${friend.friendId}"
             oncontextmenu="FriendManager.showContextMenu(${friend.friendshipId}, ${friend.friendId}, '${Utils.escapeHtml(friend.nickname)}', '${friend.blockStatus}', event); return false;">
            <div class="avatar-wrapper">
                <img src="${avatarSrc}" alt="${Utils.escapeHtml(friend.nickname)}" class="avatar avatar-md">
                <span class="status-indicator ${isBlocked ? 'offline' : statusClass}"></span>
            </div>
            <div class="list-item-content">
                <div class="list-item-header">
                    <span class="list-item-title">${Utils.escapeHtml(friend.nickname)}${blockLabel ? ' ' + blockLabel : ''}</span>
                </div>
                <div class="list-item-preview">
                    <span class="text-truncate">${Utils.escapeHtml(friend.signature || friend.username || '')}</span>
                </div>
            </div>
        </div>`;
    },

    _closeAnyModal() {
        this.closeSearchModal();
        this.closeRequestsModal();
        this.closeMoveFriendModal();
        this.closeProfileModal();
        this.closeGroupManageModal();
        this._closeContextMenu();
        // 同时关闭群成员右键菜单
        document.getElementById('memberContextMenu')?.remove();
    },

    // WebSocket 实时接收好友申请
    onNewFriendRequest(data) {
        // data 是 FriendRequestVO: { friendshipId, userId, username, nickname, avatar, onlineStatus, verificationMessage, createTime }
        this.pendingCount++;
        this._updateRequestBadge();
        // 如果好友申请弹窗已打开，刷新列表
        const modal = document.getElementById('requestsModal');
        if (modal && modal._received) {
            modal._received.unshift(data);
            const activeTab = document.querySelector('.request-tab.active');
            if (activeTab && activeTab.dataset.rtab === 'received') {
                document.getElementById('requestsModalBody').innerHTML = this._renderRequestList('received', modal._received);
                lucide.createIcons();
            }
            // 更新 tab 标题上的数字
            const receivedTab = document.querySelector('.request-tab[data-rtab="received"]');
            if (receivedTab && modal._received.length > 0) {
                receivedTab.innerHTML = `收到的申请 <span class="badge" style="margin-left:4px;">${modal._received.length}</span>`;
            }
        }
        // Toast 提示
        Utils.showToast(`${data.nickname || data.username} 发来好友申请`, 'info');
    },

    // WebSocket 实时同步拉黑状态
    onBlockStatusChange(data) {
        const myId = Auth.getUserId();
        const iAmTheBlocker = data.fromUserId === myId;
        // 更新本地 groups 数据
        for (const group of this.groups) {
            const f = (group.friends || []).find(x => x.friendshipId === data.friendshipId);
            if (f) {
                if (data.blocked) {
                    // 有人拉黑了对方
                    if (iAmTheBlocker) {
                        f.blockStatus = f.blockStatus === 'blocked_by_them' ? 'both' : 'blocked_by_me';
                    } else {
                        f.blockStatus = f.blockStatus === 'blocked_by_me' ? 'both' : 'blocked_by_them';
                    }
                } else {
                    // 有人解除拉黑
                    if (iAmTheBlocker) {
                        f.blockStatus = f.blockStatus === 'both' ? 'blocked_by_them' : 'none';
                    } else {
                        f.blockStatus = f.blockStatus === 'both' ? 'blocked_by_me' : 'none';
                    }
                }
                break;
            }
        }
        this.renderFriendList();

        // 如果当前聊天对象是被拉黑的好友，实时更新聊天头+输入框
        if (ChatManager.currentFriendInfo) {
            for (const group of this.groups) {
                const f = (group.friends || []).find(x => x.friendId === ChatManager.currentFriendInfo.friendId);
                if (f) {
                    ChatManager.currentFriendInfo.blockStatus = f.blockStatus;
                    ChatHeaderController.refreshBlockStatus(ChatManager.currentFriendId, f.blockStatus);
                    ChatManager._updateInputState();
                    break;
                }
            }
        }
    },
};
