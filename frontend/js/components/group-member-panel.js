const GroupMemberPanel = {
    currentGroupId: null,
    async show(groupId, myRole) {
        this.currentGroupId = groupId;
        const res = await API.get(`/api/group/${groupId}/members`);
        const members = (res && res.code === 200) ? res.data : [];
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `<div class="modal-container" style="max-width:400px;"><div class="modal-header"><h3>群成员 (${members.length})</h3><button class="modal-close btn-icon"><i data-lucide="x"></i></button></div>
      <div class="modal-body"><div id="memberList" style="max-height:400px;overflow-y:auto;">${members.map(m => `<div style="display:flex;align-items:center;gap:12px;padding:8px 0;border-bottom:1px solid #eee;">
        <img src="${Utils.getAvatarUrl(m.avatar)}" style="width:40px;height:40px;border-radius:50%;"><div><div><strong>${Utils.escapeHtml(m.nickname)}</strong> ${m.role === 2 ? '👑 群主' : (m.role === 1 ? '⭐ 管理员' : '成员')}</div></div></div>`).join('')}</div></div>
      <div class="modal-footer"><button class="btn btn-secondary" id="closePanelBtn">关闭</button></div></div>`;
        document.body.appendChild(modal);
        lucide.createIcons();
        modal.querySelector('.modal-close').addEventListener('click', () => modal.remove());
        document.getElementById('closePanelBtn').addEventListener('click', () => modal.remove());
    }
};