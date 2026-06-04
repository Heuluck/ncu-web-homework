const GroupInviteModal = {
    show(groupId, onSuccess) {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `<div class="modal-container" style="max-width:400px;"><div class="modal-header"><h3>邀请好友</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
      <div class="modal-body"><div id="inviteFriendList" style="max-height:300px;overflow-y:auto;"></div></div>
      <div class="modal-footer"><button class="btn btn-secondary" id="cancelInviteBtn">取消</button><button class="btn btn-primary" id="confirmInviteBtn">邀请</button></div></div>`;
        document.body.appendChild(modal);
        lucide.createIcons();

        let selectedFriends = new Set();
        let friendsList = [];

        const loadFriends = async () => {
            const res = await API.get('/api/friend/list');
            if (res && res.code === 200) {
                friendsList = res.data || [];
                renderFriendList();
            }
        };
        const renderFriendList = () => {
            const container = document.getElementById('inviteFriendList');
            if (friendsList.length === 0) { container.innerHTML = '<div class="empty-state-text">暂无好友</div>'; return; }
            container.innerHTML = friendsList.map(f => `<div class="friend-item ${selectedFriends.has(f.userId) ? 'selected' : ''}" data-user-id="${f.userId}">
        <img src="${Utils.getAvatarUrl(f.avatar)}" class="avatar avatar-sm"><span class="friend-name">${Utils.escapeHtml(f.nickname)}</span>
        <div class="friend-checkbox"><i data-lucide="${selectedFriends.has(f.userId) ? 'check-circle' : 'circle'}"></i></div></div>`).join('');
            container.querySelectorAll('.friend-item').forEach(item => {
                item.addEventListener('click', () => {
                    const userId = parseInt(item.dataset.userId);
                    selectedFriends.has(userId) ? selectedFriends.delete(userId) : selectedFriends.add(userId);
                    renderFriendList();
                });
            });
            lucide.createIcons();
        };

        const closeModal = () => modal.remove();
        modal.querySelector('.modal-close').addEventListener('click', closeModal);
        document.getElementById('cancelInviteBtn').addEventListener('click', closeModal);

        document.getElementById('confirmInviteBtn').addEventListener('click', async () => {
            if (selectedFriends.size === 0) { Utils.showToast('请选择好友', 'warning'); return; }
            const res = await API.post(`/api/group/${groupId}/invite`, { memberIds: Array.from(selectedFriends) });
            if (res && res.code === 200) {
                Utils.showToast('邀请成功', 'success');
                closeModal();
                if (onSuccess) onSuccess();
            }
        });
        loadFriends();
    }
};