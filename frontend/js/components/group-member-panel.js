const GroupMemberPanel = {
    currentGroupId: null,
    myRole: 0,
    allMembers: [],

    close() {
        document.getElementById('groupMemberPanelModal')?.remove();
    },

    async show(groupId, myRole) {
        // 先关闭旧面板
        this.close();
        this.currentGroupId = groupId;
        this.myRole = myRole || 0;
        const res = await API.get(`/api/group/${groupId}/members`);
        this.allMembers = (res && res.code === 200) ? res.data : [];
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.id = 'groupMemberPanelModal';
        modal.innerHTML = `<div class="modal-container" style="max-width:420px;overflow:hidden;">
          <div class="modal-header"><h3>群成员 (${this.allMembers.length})</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
          <div class="modal-body" style="padding:0;overflow:hidden;">
            <div style="padding:12px 16px;border-bottom:1px solid var(--border-subtle);display:flex;gap:8px;align-items:center;">
              <input type="text" id="memberSearchInput" class="input-control" placeholder="搜索成员..." style="height:36px;font-size:13px;flex:1;">
              <button class="btn btn-primary btn-sm" id="inviteMemberBtn" style="white-space:nowrap;height:36px;">
                <i data-lucide="user-plus" style="width:14px;height:14px;"></i> 邀请
              </button>
            </div>
            <div id="memberList" style="max-height:400px;overflow-y:auto;padding:4px 0;"></div>
          </div>
        </div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

        this._renderMembers(this.allMembers);

        // 搜索
        document.getElementById('memberSearchInput').addEventListener('input', (e) => {
            const keyword = e.target.value.trim().toLowerCase();
            if (!keyword) {
                this._renderMembers(this.allMembers);
            } else {
                const filtered = this.allMembers.filter(m =>
                    (m.nickname && m.nickname.toLowerCase().includes(keyword)) ||
                    (m.username && m.username.toLowerCase().includes(keyword))
                );
                this._renderMembers(filtered);
            }
        });

        // 遮罩点击关闭
        modal.addEventListener('click', (e) => { if (e.target === modal) this.close(); });
        modal.querySelector('.modal-close').addEventListener('click', () => this.close());

        // 邀请成员按钮：先关成员面板，再开邀请面板
        document.getElementById('inviteMemberBtn').addEventListener('click', () => {
            if (typeof GroupInviteModal !== 'undefined') {
                this.close();
                GroupInviteModal.show(groupId, () => {
                    // 邀请成功后重新打开成员面板
                    this.show(groupId, myRole);
                });
            }
        });
    },

    _renderMembers(members) {
        const container = document.getElementById('memberList');
        if (!container) return;
        if (members.length === 0) {
            container.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px;">暂无成员</div>';
            return;
        }
        container.innerHTML = members.map(m => {
            const avatarSrc = Utils.getAvatarUrl(m.avatar, `user-${m.userId}`);
            const roleText = m.role === 2 ? '群主' : (m.role === 1 ? '管理员' : '');
            return `
            <div class="member-item" data-user-id="${m.userId}" data-role="${m.role}" style="display:flex;align-items:center;gap:12px;padding:10px 16px;cursor:pointer;transition:background 0.15s;border-radius:var(--radius-lg);margin:0 4px;overflow:hidden;">
                <div style="position:relative;flex-shrink:0;">
                    <img src="${avatarSrc}" class="avatar avatar-sm">
                </div>
                <div style="flex:1;min-width:0;">
                    <div style="font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${Utils.escapeHtml(m.nickname)}</div>
                    ${m.username ? `<div style="font-size:11px;color:var(--text-muted);">@${Utils.escapeHtml(m.username)}</div>` : ''}
                </div>
                ${roleText ? `<span style="font-size:11px;color:var(--color-primary);font-weight:500;flex-shrink:0;">${roleText}</span>` : ''}
            </div>`;
        }).join('');

        // 给每个成员项加 hover 效果
        container.querySelectorAll('.member-item').forEach(item => {
            item.addEventListener('mouseenter', () => { item.style.background = 'var(--bg-surface-hover)'; });
            item.addEventListener('mouseleave', () => { item.style.background = ''; });
            item.addEventListener('click', (e) => {
                this._showMemberMenu(item, e);
            });
        });
    },

    _showMemberMenu(item, event) {
        // 关闭已有菜单
        document.getElementById('memberContextMenu')?.remove();

        const userId = parseInt(item.dataset.userId);
        const role = parseInt(item.dataset.role);
        const isSelf = userId === Auth.getUserId();
        // 只有群主/管理员才能移除，且不能移除自己和群主
        const canRemove = (this.myRole === 2 || this.myRole === 1) && !isSelf && role !== 2;

        const menu = document.createElement('div');
        menu.id = 'memberContextMenu';
        menu.style.cssText = `position:fixed;left:${event.clientX}px;top:${event.clientY}px;z-index:10001;background:var(--bg-app);border:1px solid var(--border-subtle);border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);min-width:150px;padding:4px 0;overflow:hidden;`;

        let html = `<div class="context-menu-item" data-action="profile" style="display:flex;align-items:center;gap:8px;padding:8px 12px;font-size:13px;cursor:pointer;">
            <i data-lucide="user" style="width:14px;height:14px;"></i> 查看资料</div>`;
        if (canRemove) {
            html += `<div class="context-menu-item" data-action="remove" style="display:flex;align-items:center;gap:8px;padding:8px 12px;font-size:13px;cursor:pointer;color:var(--color-danger);">
            <i data-lucide="user-x" style="width:14px;height:14px;"></i> 移除成员</div>`;
        }
        menu.innerHTML = html;
        document.body.appendChild(menu);
        lucide.createIcons();

        // 自动调整位置防止溢出
        const rect = menu.getBoundingClientRect();
        if (rect.right > window.innerWidth) menu.style.left = (window.innerWidth - rect.width - 8) + 'px';
        if (rect.bottom > window.innerHeight) menu.style.top = (window.innerHeight - rect.height - 8) + 'px';

        const closeMenu = () => { menu.remove(); document.removeEventListener('click', closeMenu); };
        setTimeout(() => document.addEventListener('click', closeMenu, { once: true }), 0);

        menu.querySelector('[data-action="profile"]').addEventListener('click', () => {
            closeMenu();
            if (typeof FriendManager !== 'undefined') {
                FriendManager.openProfileModal(userId);
            }
        });

        const removeBtn = menu.querySelector('[data-action="remove"]');
        if (removeBtn) {
            removeBtn.addEventListener('click', async () => {
                closeMenu();
                if (!confirm('确定移除该成员？')) return;
                const res = await API.delete(`/api/group/${this.currentGroupId}/member/${userId}`);
                if (res && res.code === 200) {
                    Utils.showToast('已移除', 'success');
                    // 刷新成员列表
                    this.allMembers = this.allMembers.filter(m => m.userId !== userId);
                    this._renderMembers(this.allMembers);
                } else {
                    Utils.showToast(res?.message || '操作失败', 'error');
                }
            });
        }
    }
};