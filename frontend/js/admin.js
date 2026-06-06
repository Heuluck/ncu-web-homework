/**
 * 管理后台 JS
 */
const AdminApp = {
  currentPanel: 'users',
  currentPage: { users: 1, groups: 1, announcements: 1, sensitive: 1 },
  pageSize: { users: 20, groups: 20, announcements: 20, sensitive: 50 },

  init() {
    this._bindNav();
    this.switchPanel('users');
  },

  _bindNav() {
    document.querySelectorAll('.admin-nav-item').forEach(btn => {
      btn.addEventListener('click', () => {
        const panel = btn.dataset.panel;
        this.switchPanel(panel);
      });
    });
  },

  switchPanel(panel) {
    this.currentPanel = panel;
    document.querySelectorAll('.admin-nav-item').forEach(b => b.classList.remove('active'));
    const active = document.querySelector(`[data-panel="${panel}"]`);
    if (active) active.classList.add('active');

    const titles = { users: '用户管理', groups: '群聊管理', announcements: '系统公告', sensitive: '敏感词管理' };
    document.getElementById('panel-title').textContent = titles[panel];

    switch (panel) {
      case 'users': this._renderUsers(); break;
      case 'groups': this._renderGroups(); break;
      case 'announcements': this._renderAnnouncements(); break;
      case 'sensitive': this._renderSensitiveWords(); break;
    }
  },

  // ========== 用户管理 ==========
  async _renderUsers() {
    const container = document.getElementById('panel-content');
    container.innerHTML = `
      <div class="admin-toolbar">
        <input class="admin-search" id="user-search" placeholder="搜索用户名 / 昵称..." oninput="AdminApp._searchUsers()">
        <select id="user-status-filter" onchange="AdminApp._searchUsers()" class="btn btn-sm btn-secondary">
          <option value="">全部状态</option>
          <option value="1">在线</option>
          <option value="0">离线</option>
          <option value="2">忙碌</option>
          <option value="3">勿扰</option>
        </select>
      </div>
      <div id="user-table-wrap"></div>
      <div id="user-pagination" class="admin-pagination"></div>
    `;
    await this._loadUsers();
  },

  async _loadUsers() {
    const keyword = document.getElementById('user-search')?.value || '';
    const status = document.getElementById('user-status-filter')?.value || '';
    const page = this.currentPage.users;
    const size = this.pageSize.users;
    const params = new URLSearchParams({ pageNum: page, pageSize: size });
    if (keyword) params.set('keyword', keyword);
    if (status !== '') params.set('status', status);

    const res = await API.get(`/api/admin/users?${params}`);
    if (res.code !== 200) return Utils.showToast(res.message, 'error');

    const { records, total } = res.data;
    const wrap = document.getElementById('user-table-wrap');
    if (!records || records.length === 0) {
      wrap.innerHTML = '<div class="admin-empty"><i data-lucide="users"></i><p>暂无用户数据</p></div>';
      lucide.createIcons();
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>用户</th><th>状态</th><th>角色</th><th>注册时间</th><th>操作</th></tr></thead>
        <tbody>${records.map(u => `
          <tr>
            <td>
              <div class="user-cell">
                <img class="avatar avatar-sm" src="${Utils.getAvatarUrl(u.avatar, u.nickname)}" alt="" onerror="this.src='${Utils.getAvatarUrl(null, 'user')}'">
                <div class="user-info">
                  <span class="user-nick">${Utils.escapeHtml(u.nickname)}</span>
                  <span class="user-name">@${Utils.escapeHtml(u.username)}</span>
                </div>
              </div>
            </td>
            <td>
              <span class="admin-status ${u.enabled === 0 ? 'disabled' : (u.status === 1 ? 'online' : 'offline')}">
                ${u.enabled === 0 ? '已禁用' : Utils.getStatusText(u.status)}
              </span>
            </td>
            <td><span class="admin-badge ${u.role === 1 ? 'admin' : 'user'}">${u.role === 1 ? '管理员' : '用户'}</span></td>
            <td>${Utils.formatTime(u.createTime)}</td>
            <td class="admin-actions">
              <button class="btn btn-sm ${u.enabled === 1 ? 'btn-danger' : 'btn-success'}"
                onclick="AdminApp._toggleUser(${u.id}, ${u.enabled === 1 ? 0 : 1})">
                ${u.enabled === 1 ? '禁用' : '启用'}
              </button>
              <button class="btn btn-sm btn-secondary" onclick="AdminApp._resetPwd(${u.id}, '${Utils.escapeHtml(u.nickname)}')">重置密码</button>
            </td>
          </tr>
        `).join('')}</tbody>
      </table>
    `;
    this._renderPagination('user-pagination', 'users', total, size, page, () => this._loadUsers());
  },

  _searchUsers() {
    this.currentPage.users = 1;
    Utils.debounce(() => this._loadUsers(), 300)();
  },

  async _toggleUser(userId, enabled) {
    const res = await API.put(`/api/admin/users/${userId}/toggle-status`, { enabled });
    if (res.code === 200) {
      Utils.showToast('操作成功', 'success');
      this._loadUsers();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  async _resetPwd(userId, name) {
    if (!await Utils.showConfirm(`确定将用户 "${name}" 的密码重置为 123456 吗？`, { title: '重置密码', confirmText: '重置', danger: true })) return;
    const res = await API.put(`/api/admin/users/${userId}/reset-password`, {});
    if (res.code === 200) {
      Utils.showToast('密码已重置为 123456', 'success');
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  // ========== 群聊管理 ==========
  async _renderGroups() {
    const container = document.getElementById('panel-content');
    container.innerHTML = `
      <div class="admin-toolbar">
        <input class="admin-search" id="group-search" placeholder="搜索群名称..." oninput="AdminApp._searchGroups()">
      </div>
      <div id="group-table-wrap"></div>
      <div id="group-pagination" class="admin-pagination"></div>
    `;
    await this._loadGroups();
  },

  async _loadGroups() {
    const keyword = document.getElementById('group-search')?.value || '';
    const page = this.currentPage.groups;
    const size = this.pageSize.groups;
    const params = new URLSearchParams({ pageNum: page, pageSize: size });
    if (keyword) params.set('keyword', keyword);

    const res = await API.get(`/api/admin/groups?${params}`);
    if (res.code !== 200) return Utils.showToast(res.message, 'error');

    const { records, total } = res.data;
    const wrap = document.getElementById('group-table-wrap');
    if (!records || records.length === 0) {
      wrap.innerHTML = '<div class="admin-empty"><i data-lucide="message-square"></i><p>暂无群聊数据</p></div>';
      lucide.createIcons();
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>群聊</th><th>群主</th><th>成员数</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>${records.map(g => `
          <tr>
            <td>
              <div class="user-cell">
                <img class="avatar avatar-sm" src="${Utils.getAvatarUrl(g.avatar, g.name)}" alt="" onerror="this.src='${Utils.getAvatarUrl(null, 'group')}'">
                <div class="user-info">
                  <span class="user-nick">${Utils.escapeHtml(g.name)}</span>
                  <span class="user-name">ID: ${g.id}</span>
                </div>
              </div>
            </td>
            <td>${Utils.escapeHtml(g.ownerName || '未知')}</td>
            <td>${g.memberCount}</td>
            <td>${Utils.formatTime(g.createTime)}</td>
            <td class="admin-actions">
              <button class="btn btn-sm btn-danger" onclick="AdminApp._disbandGroup(${g.id}, '${Utils.escapeHtml(g.name)}')">解散</button>
            </td>
          </tr>
        `).join('')}</tbody>
      </table>
    `;
    this._renderPagination('group-pagination', 'groups', total, size, page, () => this._loadGroups());
  },

  _searchGroups() {
    this.currentPage.groups = 1;
    Utils.debounce(() => this._loadGroups(), 300)();
  },

  async _disbandGroup(id, name) {
    if (!await Utils.showConfirm(`确定解散群聊 "${name}" 吗？此操作不可撤销。`, { title: '解散群聊', confirmText: '解散', danger: true })) return;
    const res = await API.delete(`/api/admin/groups/${id}`);
    if (res.code === 200) {
      Utils.showToast('群聊已解散', 'success');
      this._loadGroups();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  // ========== 公告管理 ==========
  async _renderAnnouncements() {
    const container = document.getElementById('panel-content');
    container.innerHTML = `
      <div class="admin-toolbar">
        <button class="btn btn-primary btn-sm" onclick="AdminApp._openAnnouncementModal()">
          <i data-lucide="plus"></i> 新建公告
        </button>
      </div>
      <div id="announcement-list"></div>
      <div id="announcement-pagination" class="admin-pagination"></div>
    `;
    await this._loadAnnouncements();
    lucide.createIcons();
  },

  async _loadAnnouncements() {
    const page = this.currentPage.announcements;
    const size = this.pageSize.announcements;
    const res = await API.get(`/api/admin/announcements?pageNum=${page}&pageSize=${size}`);
    if (res.code !== 200) return;

    const { records, total } = res.data;
    const wrap = document.getElementById('announcement-list');
    if (!records || records.length === 0) {
      wrap.innerHTML = '<div class="admin-empty"><i data-lucide="megaphone"></i><p>暂无公告</p></div>';
      lucide.createIcons();
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>标题</th><th>发布者</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>${records.map(a => `
          <tr>
            <td><strong>${Utils.escapeHtml(a.title)}</strong></td>
            <td>${Utils.escapeHtml(a.publisherName || '未知')}</td>
            <td><span class="admin-badge ${a.isPublished ? 'published' : 'draft'}">${a.isPublished ? '已发布' : '草稿'}</span></td>
            <td>${Utils.formatTime(a.updateTime || a.createTime)}</td>
            <td class="admin-actions">
              <button class="btn btn-sm btn-secondary" onclick='AdminApp._openAnnouncementModal(${JSON.stringify(a)})'>编辑</button>
              <button class="btn btn-sm btn-ghost" onclick="AdminApp._toggleAnnounce(${a.id}, ${a.isPublished ? 0 : 1})">
                ${a.isPublished ? '撤回' : '发布'}
              </button>
              <button class="btn btn-sm btn-danger" onclick="AdminApp._deleteAnnounce(${a.id}, '${Utils.escapeHtml(a.title)}')">删除</button>
            </td>
          </tr>
        `).join('')}</tbody>
      </table>
    `;
    this._renderPagination('announcement-pagination', 'announcements', total, size, page, () => this._loadAnnouncements());
  },

  _openAnnouncementModal(data) {
    const isEdit = !!data;
    const overlay = document.getElementById('admin-modal-overlay');
    const content = document.getElementById('admin-modal-content');
    content.innerHTML = `
      <h3>${isEdit ? '编辑公告' : '新建公告'}</h3>
      <label>标题</label>
      <input id="ann-title" value="${isEdit ? Utils.escapeHtml(data.title) : ''}" placeholder="请输入公告标题">
      <label>内容</label>
      <textarea id="ann-content" placeholder="请输入公告内容">${isEdit ? data.content : ''}</textarea>
      <label>
        <input type="checkbox" id="ann-published" ${isEdit && data.isPublished ? 'checked' : ''}> 立即发布
      </label>
      <div class="modal-actions">
        <button class="btn btn-secondary btn-sm" onclick="AdminApp._closeModal()">取消</button>
        <button class="btn btn-primary btn-sm" onclick="AdminApp._saveAnnouncement(${isEdit ? data.id : 'null'})">保存</button>
      </div>
    `;
    overlay.style.display = 'flex';
  },

  async _saveAnnouncement(id) {
    const title = document.getElementById('ann-title').value.trim();
    const content = document.getElementById('ann-content').value.trim();
    const isPublished = document.getElementById('ann-published').checked ? 1 : 0;
    if (!title) return Utils.showToast('请输入标题', 'error');

    const body = { title, content, isPublished };
    const res = id
      ? await API.put(`/api/admin/announcements/${id}`, body)
      : await API.post('/api/admin/announcements', body);

    if (res.code === 200) {
      Utils.showToast('保存成功', 'success');
      this._closeModal();
      this._loadAnnouncements();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  async _toggleAnnounce(id, isPublished) {
    const res = await API.put(`/api/admin/announcements/${id}`, { isPublished });
    if (res.code === 200) {
      Utils.showToast(isPublished ? '已发布' : '已撤回', 'success');
      this._loadAnnouncements();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  async _deleteAnnounce(id, title) {
    if (!await Utils.showConfirm(`确定删除公告 "${title}" 吗？`, { title: '删除公告', confirmText: '删除', danger: true })) return;
    const res = await API.delete(`/api/admin/announcements/${id}`);
    if (res.code === 200) {
      Utils.showToast('删除成功', 'success');
      this._loadAnnouncements();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  _closeModal() {
    document.getElementById('admin-modal-overlay').style.display = 'none';
    document.getElementById('admin-modal-content').innerHTML = '';
  },

  // ========== 敏感词管理 ==========
  async _renderSensitiveWords() {
    const container = document.getElementById('panel-content');
    container.innerHTML = `
      <div class="sensitive-test">
        <label>🔍 敏感词检测</label>
        <textarea id="sensitive-test-input" placeholder="输入文本以检测敏感词..." oninput="AdminApp._testSensitive()"></textarea>
        <div id="sensitive-test-result"></div>
      </div>
      <div class="admin-toolbar">
        <input class="admin-search" id="sw-search" placeholder="搜索敏感词..." oninput="AdminApp._searchSensitive()">
        <button class="btn btn-primary btn-sm" onclick="AdminApp._openSensitiveModal()">
          <i data-lucide="plus"></i> 添加
        </button>
      </div>
      <div id="sensitive-list"></div>
      <div id="sensitive-pagination" class="admin-pagination"></div>
    `;
    await this._loadSensitiveWords();
    lucide.createIcons();
  },

  async _loadSensitiveWords() {
    const keyword = document.getElementById('sw-search')?.value || '';
    const page = this.currentPage.sensitive;
    const size = this.pageSize.sensitive;
    const params = new URLSearchParams({ pageNum: page, pageSize: size });
    if (keyword) params.set('keyword', keyword);

    const res = await API.get(`/api/admin/sensitive-words?${params}`);
    if (res.code !== 200) return;

    const { records, total } = res.data;
    const wrap = document.getElementById('sensitive-list');
    if (!records || records.length === 0) {
      wrap.innerHTML = '<div class="admin-empty"><i data-lucide="shield-alert"></i><p>暂无敏感词</p></div>';
      lucide.createIcons();
      return;
    }

    wrap.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>敏感词</th><th>分类</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>${records.map(sw => `
          <tr>
            <td><code style="background:var(--bg-surface);padding:2px 8px;border-radius:4px;font-size:13px">${Utils.escapeHtml(sw.word)}</code></td>
            <td>${Utils.escapeHtml(sw.category)}</td>
            <td><span class="admin-badge ${sw.enabled ? 'published' : 'draft'}">${sw.enabled ? '启用' : '禁用'}</span></td>
            <td>${Utils.formatTime(sw.createTime)}</td>
            <td class="admin-actions">
              <button class="btn btn-sm btn-secondary" onclick='AdminApp._openSensitiveModal(${JSON.stringify(sw)})'>编辑</button>
              <button class="btn btn-sm btn-danger" onclick="AdminApp._deleteSensitive(${sw.id}, '${Utils.escapeHtml(sw.word)}')">删除</button>
            </td>
          </tr>
        `).join('')}</tbody>
      </table>
    `;
    this._renderPagination('sensitive-pagination', 'sensitive', total, size, page, () => this._loadSensitiveWords());
  },

  _searchSensitive() {
    this.currentPage.sensitive = 1;
    Utils.debounce(() => this._loadSensitiveWords(), 300)();
  },

  _openSensitiveModal(data) {
    const isEdit = !!data;
    const overlay = document.getElementById('admin-modal-overlay');
    const content = document.getElementById('admin-modal-content');
    content.innerHTML = `
      <h3>${isEdit ? '编辑敏感词' : '添加敏感词'}</h3>
      <label>敏感词</label>
      <input id="sw-word" value="${isEdit ? data.word : ''}" placeholder="请输入敏感词">
      <label>分类</label>
      <input id="sw-category" value="${isEdit ? (data.category || '通用') : '通用'}" placeholder="如：政治、色情、暴力">
      <label>
        <input type="checkbox" id="sw-enabled" ${(!isEdit) || data.enabled ? 'checked' : ''}> 启用
      </label>
      <div class="modal-actions">
        <button class="btn btn-secondary btn-sm" onclick="AdminApp._closeModal()">取消</button>
        <button class="btn btn-primary btn-sm" onclick="AdminApp._saveSensitive(${isEdit ? data.id : 'null'})">保存</button>
      </div>
    `;
    overlay.style.display = 'flex';
  },

  async _saveSensitive(id) {
    const word = document.getElementById('sw-word').value.trim();
    const category = document.getElementById('sw-category').value.trim() || '通用';
    const enabled = document.getElementById('sw-enabled').checked ? 1 : 0;
    if (!word) return Utils.showToast('请输入敏感词', 'error');

    const body = { word, category, enabled };
    const res = id
      ? await API.put(`/api/admin/sensitive-words/${id}`, body)
      : await API.post('/api/admin/sensitive-words', body);

    if (res.code === 200) {
      Utils.showToast('保存成功', 'success');
      this._closeModal();
      this._loadSensitiveWords();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  async _deleteSensitive(id, word) {
    if (!await Utils.showConfirm(`确定删除敏感词 "${word}" 吗？`, { title: '删除敏感词', confirmText: '删除', danger: true })) return;
    const res = await API.delete(`/api/admin/sensitive-words/${id}`);
    if (res.code === 200) {
      Utils.showToast('删除成功', 'success');
      this._loadSensitiveWords();
    } else {
      Utils.showToast(res.message, 'error');
    }
  },

  async _testSensitive() {
    const text = document.getElementById('sensitive-test-input').value.trim();
    const resultEl = document.getElementById('sensitive-test-result');
    if (!text) { resultEl.innerHTML = ''; return; }

    const res = await API.post('/api/admin/sensitive-words/check', { text });
    if (res.code !== 200) return;

    const { hasSensitive, words } = res.data;
    if (hasSensitive) {
      let html = Utils.escapeHtml(text);
      words.forEach(w => {
        html = html.replace(new RegExp(Utils.escapeHtml(w).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'),
          `<span class="highlight-word">${w}</span>`);
      });
      resultEl.innerHTML = `
        <div class="sensitive-result found">
          ⚠️ 检测到 ${words.length} 个敏感词：[${words.join(', ')}]<br>
          ${html}
        </div>`;
    } else {
      resultEl.innerHTML = '<div class="sensitive-result clean">✅ 未检测到敏感词</div>';
    }
  },

  // ========== 分页 ==========
  _renderPagination(elId, panel, total, size, page, loadFn) {
    const el = document.getElementById(elId);
    if (!el) return;
    const totalPages = Math.ceil(total / size);
    if (totalPages <= 1) { el.innerHTML = ''; return; }

    el.innerHTML = `
      <button ${page <= 1 ? 'disabled' : ''} onclick="AdminApp._goPage('${panel}', ${page - 1})">上一页</button>
      <span>第 ${page} / ${totalPages} 页（共 ${total} 条）</span>
      <button ${page >= totalPages ? 'disabled' : ''} onclick="AdminApp._goPage('${panel}', ${page + 1})">下一页</button>
    `;
  },

  _goPage(panel, page) {
    this.currentPage[panel] = page;
    switch (panel) {
      case 'users': this._loadUsers(); break;
      case 'groups': this._loadGroups(); break;
      case 'announcements': this._loadAnnouncements(); break;
      case 'sensitive': this._loadSensitiveWords(); break;
    }
  }
};
