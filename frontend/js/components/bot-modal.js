/**
 * AI 机器人管理弹窗
 * 列表视图：群内机器人（编辑/复制/移除）+ 添加按钮
 * 我的机器人视图：管理所有机器人（编辑/复制/删除/添加到群）
 */
const BotModal = {
    groupId: null,
    groupInfo: null,
    isOwner: false,
    isAdmin: false,
    canManage: false, // 群主或管理员
    currentView: 'list', // 'list' | 'myBots' | 'form'

    async show(groupId) {
        this.groupId = groupId;
        const infoRes = await API.get(`/api/group/info/${groupId}`);
        this.groupInfo = (infoRes && infoRes.code === 200) ? infoRes.data : {};
        this.isOwner = this.groupInfo.myRole === 2;
        this.isAdmin = this.groupInfo.myRole === 1;
        this.canManage = this.groupInfo.myRole >= 1;
        await BotManager.loadGroupBots(groupId);
        await BotManager.loadMyBots();
        this._renderListView();
    },

    // ==================== 群内机器人列表 ====================

    _renderListView() {
        this.currentView = 'list';
        this._removeExisting();
        const bots = BotManager.groupBots;
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.id = 'botModalOverlay';

        let botsHtml = '';
        if (bots.length === 0) {
            botsHtml = `<div class="empty-state" style="padding:30px 0;">
                <div class="empty-state-icon"><i data-lucide="bot" style="width:36px;height:36px;"></i></div>
                <div class="empty-state-title" style="margin-top:8px;">暂无 AI 机器人</div>
                <div class="empty-state-text">点击下方按钮添加机器人</div>
            </div>`;
        } else {
            botsHtml = bots.map(bot => {
                const avatar = bot.avatar || BotManager.getDefaultBotAvatar(bot.name);
                const triggerLabel = BotManager.getTriggerLabel(bot.triggerType);
                const isMyBot = bot.ownerId === Auth.getUserId();
                let actions = '';
                if (isMyBot) {
                    actions += `<button class="btn btn-ghost btn-sm bot-edit-btn" data-bot-id="${bot.id}" title="编辑"><i data-lucide="pencil" style="width:13px;height:13px;"></i></button>`;
                    actions += `<button class="btn btn-ghost btn-sm bot-copy-btn" data-bot-id="${bot.id}" title="复制"><i data-lucide="copy" style="width:13px;height:13px;"></i></button>`;
                }
                // 群主可移除任何机器人；添加者可移除；机器人主人可移除
                const canRemove = this.isOwner || bot.addedBy === Auth.getUserId() || isMyBot;
                if (canRemove) {
                    actions += `<button class="btn btn-ghost btn-sm bot-remove-btn" data-bot-id="${bot.id}" title="从群聊移除"><i data-lucide="x" style="width:13px;height:13px;"></i></button>`;
                }
                return `<div class="bot-card" data-bot-id="${bot.id}">
                    <div class="bot-card-left">
                        <img src="${Utils.escapeHtml(avatar)}" class="avatar avatar-sm" onerror="this.src='${BotManager.getDefaultBotAvatar(bot.name)}'">
                        <div class="bot-card-info">
                            <div class="bot-card-name">🤖 ${Utils.escapeHtml(bot.name)}</div>
                            <div class="bot-card-meta">${Utils.escapeHtml(bot.model)} · ${triggerLabel}</div>
                        </div>
                    </div>
                    <div class="bot-card-right" style="display:flex;gap:2px;">${actions}</div>
                </div>`;
            }).join('');
        }

        modal.innerHTML = `<div class="modal-container" style="max-width:480px;">
            <div class="modal-header">
                <h3>🤖 群内 AI 机器人</h3>
                <button class="modal-close btn-icon"><i data-lucide="x"></i></button>
            </div>
            <div class="modal-body" style="padding:var(--space-4) var(--space-6);">
                ${botsHtml}
            </div>
            <div class="modal-footer" style="flex-direction:column;gap:8px;">
                ${this.canManage ? `<button class="btn btn-primary btn-sm" id="botAddBtn" style="width:100%;"><i data-lucide="plus" style="width:14px;height:14px;"></i> 添加机器人</button>` : ''}
                <button class="btn btn-ghost btn-sm" id="botMyBotsBtn" style="width:100%;"><i data-lucide="settings" style="width:14px;height:14px;"></i> ${this.canManage ? '添加我现有的机器人' : '管理我的机器人'}</button>
                <button class="btn btn-secondary btn-sm" id="botCloseBtn" style="width:100%;">关闭</button>
            </div>
        </div>`;

        document.body.appendChild(modal);
        lucide.createIcons();

        this._bindCommon(modal);
        document.getElementById('botCloseBtn').addEventListener('click', () => Utils.closeModalAnimated(modal));
        document.getElementById('botMyBotsBtn').addEventListener('click', () => this._renderMyBotsView());

        modal.querySelectorAll('.bot-remove-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                if (!await Utils.showConfirm('确定从群聊移除此机器人？', { title: '移除机器人', confirmText: '移除', danger: true })) return;
                await BotManager.removeBotFromGroup(parseInt(btn.dataset.botId), this.groupId);
                await BotManager.loadGroupBots(this.groupId);
                this._renderListView();
            });
        });
        modal.querySelectorAll('.bot-edit-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const bot = BotManager.myBots.find(b => b.id === parseInt(btn.dataset.botId));
                if (bot) this._renderFormView('edit', bot);
            });
        });
        modal.querySelectorAll('.bot-copy-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const bot = BotManager.myBots.find(b => b.id === parseInt(btn.dataset.botId));
                if (bot) this._renderFormView('copy', bot);
            });
        });

        const addBtnEl = document.getElementById('botAddBtn');
        if (addBtnEl) addBtnEl.addEventListener('click', () => this._renderFormView('create'));
    },

    // ==================== 我的机器人管理列表 ====================

    _renderMyBotsView() {
        this.currentView = 'myBots';
        this._removeExisting();
        const myBots = BotManager.myBots;
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.id = 'botModalOverlay';

        let listHtml = '';
        if (myBots.length === 0) {
            listHtml = `<div class="empty-state" style="padding:30px 0;">
                <div class="empty-state-icon"><i data-lucide="bot" style="width:36px;height:36px;"></i></div>
                <div class="empty-state-title" style="margin-top:8px;">你还没有创建机器人</div>
            </div>`;
        } else {
            const groupBotIds = new Set(BotManager.groupBots.map(b => b.id));
            listHtml = myBots.map(bot => {
                const avatar = bot.avatar || BotManager.getDefaultBotAvatar(bot.name);
                const triggerLabel = BotManager.getTriggerLabel(bot.triggerType);
                const inGroup = groupBotIds.has(bot.id);
                let actions = '';
                actions += `<button class="btn btn-ghost btn-sm mybot-edit-btn" data-bot-id="${bot.id}" title="编辑"><i data-lucide="pencil" style="width:13px;height:13px;"></i></button>`;
                actions += `<button class="btn btn-ghost btn-sm mybot-copy-btn" data-bot-id="${bot.id}" title="复制"><i data-lucide="copy" style="width:13px;height:13px;"></i></button>`;
                actions += `<button class="btn btn-ghost btn-sm mybot-delete-btn" data-bot-id="${bot.id}" title="删除"><i data-lucide="trash-2" style="width:13px;height:13px;"></i></button>`;
                if (this.canManage && !inGroup) {
                    actions += `<button class="btn btn-ghost btn-sm mybot-add-btn" data-bot-id="${bot.id}" title="添加到当前群聊"><i data-lucide="plus-circle" style="width:13px;height:13px;"></i></button>`;
                }
                const statusTag = inGroup ? '<span class="text-muted" style="font-size:10px;">已在群中</span>' : '';
                return `<div class="bot-card" data-bot-id="${bot.id}">
                    <div class="bot-card-left">
                        <img src="${Utils.escapeHtml(avatar)}" class="avatar avatar-sm" onerror="this.src='${BotManager.getDefaultBotAvatar(bot.name)}'">
                        <div class="bot-card-info">
                            <div class="bot-card-name">🤖 ${Utils.escapeHtml(bot.name)} ${statusTag}</div>
                            <div class="bot-card-meta">${Utils.escapeHtml(bot.model)} · ${triggerLabel}</div>
                        </div>
                    </div>
                    <div class="bot-card-right" style="display:flex;gap:2px;">${actions}</div>
                </div>`;
            }).join('');
        }

        modal.innerHTML = `<div class="modal-container" style="max-width:480px;">
            <div class="modal-header">
                <h3>🤖 我的机器人</h3>
                <button class="modal-close btn-icon"><i data-lucide="x"></i></button>
            </div>
            <div class="modal-body" style="padding:var(--space-4) var(--space-6);">
                ${listHtml}
            </div>
            <div class="modal-footer" style="flex-direction:column;gap:8px;">
                <button class="btn btn-primary btn-sm" id="mybotCreateBtn" style="width:100%;"><i data-lucide="plus" style="width:14px;height:14px;"></i> 新建机器人</button>
                <button class="btn btn-secondary btn-sm" id="mybotBackBtn" style="width:100%;">返回群内机器人</button>
            </div>
        </div>`;

        document.body.appendChild(modal);
        lucide.createIcons();

        this._bindCommon(modal);
        document.getElementById('mybotBackBtn').addEventListener('click', () => this._renderListView());
        document.getElementById('mybotCreateBtn').addEventListener('click', () => this._renderFormView('create'));

        // 编辑
        modal.querySelectorAll('.mybot-edit-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const bot = BotManager.myBots.find(b => b.id === parseInt(btn.dataset.botId));
                if (bot) this._renderFormView('edit', bot);
            });
        });
        // 复制
        modal.querySelectorAll('.mybot-copy-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const bot = BotManager.myBots.find(b => b.id === parseInt(btn.dataset.botId));
                if (bot) this._renderFormView('copy', bot);
            });
        });
        // 删除
        modal.querySelectorAll('.mybot-delete-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const botId = parseInt(btn.dataset.botId);
                const bot = BotManager.myBots.find(b => b.id === botId);
                if (!await Utils.showConfirm(`确定彻底删除机器人 "${bot ? bot.name : ''}" 吗？此操作不可恢复。`, { title: '删除机器人', confirmText: '删除', danger: true })) return;
                const ok = await BotManager.deleteBot(botId);
                if (ok) {
                    await BotManager.loadMyBots();
                    await BotManager.loadGroupBots(this.groupId);
                    this._renderMyBotsView();
                }
            });
        });
        // 添加到群
        modal.querySelectorAll('.mybot-add-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const botId = parseInt(btn.dataset.botId);
                const ok = await BotManager.addBotToGroup(botId, this.groupId);
                if (ok) {
                    await BotManager.loadGroupBots(this.groupId);
                    this._renderMyBotsView();
                }
            });
        });
    },

    // ==================== 创建/编辑/复制表单 ====================

    _renderFormView(mode, existingBot = null) {
        this.currentView = 'form';
        this._removeExisting();
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.id = 'botModalOverlay';

        const isEdit = mode === 'edit';
        const title = isEdit ? '编辑机器人' : (mode === 'copy' ? '复制机器人' : '创建机器人');
        const submitText = isEdit ? '保存修改' : '创建并添加到群聊';

        const p = existingBot || {};
        const pName = isEdit ? (p.name || '') : (mode === 'copy' ? (p.name || '') + ' (副本)' : '');
        const pAvatar = p.avatar || '';
        const pEndpoint = p.endpoint || '';
        const pModel = p.model || 'deepseek-v4-flash';
        const pSystemPrompt = p.systemPrompt || '你是一个友好的AI助手，请用中文回答问题。';
        const pTriggerType = p.triggerType != null ? p.triggerType : 0;
        const pTriggerProb = p.triggerProbability || '';
        const pTemp = p.temperature != null ? p.temperature : 1.0;
        const pTopP = p.topP != null ? p.topP : 1.0;

        const isCopy = mode === 'copy';
        const apiKeyLabel = (isEdit || isCopy)
            ? `<label class="form-label">API Key（留空不变）</label>`
            : `<label class="form-label">API Key <span class="text-danger">*</span></label>`;
        const apiKeyPlaceholder = (isEdit || isCopy) ? '不修改请留空' : 'sk-...';

        modal.innerHTML = `<div class="modal-container" style="max-width:520px;max-height:90vh;">
            <div class="modal-header">
                <h3>${title}</h3>
                <button class="modal-close btn-icon"><i data-lucide="x"></i></button>
            </div>
            <div class="modal-body" style="max-height:calc(90vh - 140px);overflow-y:auto;padding:var(--space-4) var(--space-6);">
                <div class="bot-form">
                    <div class="form-group">
                        <label class="form-label">机器人名称 <span class="text-danger">*</span></label>
                        <input type="text" id="botName" class="input-control" placeholder="如：DeepSeek 助手" maxlength="50" value="${Utils.escapeHtml(pName)}">
                    </div>
                    <div class="form-group">
                        <label class="form-label">头像</label>
                        <div class="profile-avatar-section" style="flex-direction:row;gap:12px;align-items:center;margin-bottom:8px;">
                            <div class="profile-avatar-wrapper" style="cursor:pointer;position:relative;" onclick="document.getElementById('botAvatarInput').click()">
                                <img id="botAvatarPreview" src="${pAvatar || BotManager.getDefaultBotAvatar(pName)}" alt="头像" class="avatar avatar-lg">
                                <div class="profile-avatar-overlay">
                                    <i data-lucide="camera" style="width:20px;height:20px;"></i>
                                </div>
                            </div>
                            <span class="text-muted text-sm" style="line-height:1.4;">点击上传头像<br>或下方输入URL</span>
                        </div>
                        <input type="file" id="botAvatarInput" accept="image/*" style="display:none;">
                        <input type="text" id="botAvatar" class="input-control" placeholder="https://... 或留空使用默认头像" value="${Utils.escapeHtml(pAvatar)}">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Endpoint <span class="text-danger">*</span></label>
                        <input type="text" id="botEndpoint" class="input-control" placeholder="https://api.deepseek.com" value="${Utils.escapeHtml(pEndpoint)}">
                    </div>
                    <div class="form-group">
                        ${apiKeyLabel}
                        <div style="position:relative;">
                            <input type="password" id="botApiKey" class="input-control" placeholder="${apiKeyPlaceholder}" autocomplete="off">
                            ${(isEdit || isCopy) ? `<span style="position:absolute;right:12px;top:50%;transform:translateY(-50%);pointer-events:none;color:var(--text-muted);font-size:11px;">🔒 已加密</span>` : ''}
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">模型名称 <span class="text-danger">*</span></label>
                        <input type="text" id="botModel" class="input-control" placeholder="deepseek-v4-flash" value="${Utils.escapeHtml(pModel)}">
                    </div>
                    <div class="form-group">
                        <label class="form-label">系统提示词</label>
                        <textarea id="botSystemPrompt" class="input-control" rows="3" style="resize:vertical;">${Utils.escapeHtml(pSystemPrompt)}</textarea>
                    </div>
                    <div class="form-group">
                        <label class="form-label">触发条件</label>
                        <select id="botTriggerType" class="input-control">
                            <option value="0" ${pTriggerType===0?'selected':''}>@机器人名称 触发</option>
                            <option value="1" ${pTriggerType===1?'selected':''}>每条消息都触发</option>
                            <option value="2" ${pTriggerType===2?'selected':''}>随机概率触发</option>
                        </select>
                        <div style="font-size:11px;color:var(--text-muted);margin-top:4px;">💡 无论选哪种，@机器人名称 始终会触发回复</div>
                    </div>
                    <div class="form-group" id="botProbabilityGroup" style="display:${pTriggerType===2?'':'none'};">
                        <label class="form-label">触发概率（0~1）</label>
                        <input type="number" id="botProbability" class="input-control" min="0" max="1" step="0.01" value="${pTriggerProb}" placeholder="0.1">
                    </div>
                    <details class="bot-advanced-settings">
                        <summary class="section-title" style="cursor:pointer;user-select:none;margin-bottom:8px;">
                            <i data-lucide="settings-2" style="width:14px;height:14px;display:inline;vertical-align:middle;"></i> 高级设置
                        </summary>
                        <div style="padding-top:8px;">
                            <div class="form-group">
                                <label class="form-label">Temperature（0~2）</label>
                                <input type="number" id="botTemperature" class="input-control" min="0" max="2" step="0.1" value="${pTemp}">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Top P（0~1）</label>
                                <input type="number" id="botTopP" class="input-control" min="0" max="1" step="0.05" value="${pTopP}">
                            </div>
                        </div>
                    </details>
                </div>
            </div>
            <div class="modal-footer" style="flex-direction:column;gap:8px;">
                <button class="btn btn-primary" id="botSubmitBtn" style="width:100%;">${submitText}</button>
                <button class="btn btn-secondary" id="botFormBackBtn" style="width:100%;">返回</button>
            </div>
        </div>`;

        document.body.appendChild(modal);
        lucide.createIcons();

        // 头像上传
        const avatarInput = document.getElementById('botAvatarInput');
        const avatarPreview = document.getElementById('botAvatarPreview');
        const avatarUrlInput = document.getElementById('botAvatar');
        if (avatarInput) {
            avatarInput.addEventListener('change', async () => {
                const file = avatarInput.files[0];
                if (!file) return;
                const result = await API.upload(file);
                if (result && result.code === 200) {
                    avatarPreview.src = result.data.url;
                    avatarUrlInput.value = result.data.url;
                }
            });
        }
        // 手动输入 URL 时同步预览
        if (avatarUrlInput) {
            avatarUrlInput.addEventListener('input', () => {
                const url = avatarUrlInput.value.trim();
                if (url) avatarPreview.src = url;
                else avatarPreview.src = BotManager.getDefaultBotAvatar(document.getElementById('botName')?.value || 'bot');
            });
        }

        document.getElementById('botTriggerType').addEventListener('change', (e) => {
            document.getElementById('botProbabilityGroup').style.display = e.target.value === '2' ? '' : 'none';
        });

        this._bindCommon(modal);
        document.getElementById('botFormBackBtn').addEventListener('click', () => this._renderListView());

        document.getElementById('botSubmitBtn').addEventListener('click', async () => {
            const name = document.getElementById('botName').value.trim();
            const endpoint = document.getElementById('botEndpoint').value.trim();
            const apiKey = document.getElementById('botApiKey').value.trim();
            const model = document.getElementById('botModel').value.trim();

            if (!name) { Utils.showToast('请输入机器人名称', 'error'); return; }
            if (!endpoint) { Utils.showToast('请输入 Endpoint', 'error'); return; }
            if (!isEdit && !isCopy && !apiKey) { Utils.showToast('请输入 API Key', 'error'); return; }
            if (!model) { Utils.showToast('请输入模型名称', 'error'); return; }

            const triggerType = parseInt(document.getElementById('botTriggerType').value);
            let triggerProbability = null;
            if (triggerType === 2) {
                triggerProbability = parseFloat(document.getElementById('botProbability').value);
                if (isNaN(triggerProbability) || triggerProbability < 0 || triggerProbability > 1) { Utils.showToast('触发概率需在 0~1 之间', 'error'); return; }
            }

            const dto = {
                name,
                avatar: document.getElementById('botAvatar').value.trim() || null,
                endpoint,
                model,
                systemPrompt: document.getElementById('botSystemPrompt').value,
                triggerType,
                triggerProbability: triggerProbability,
                temperature: parseFloat(document.getElementById('botTemperature').value) || 1.0,
                topP: parseFloat(document.getElementById('botTopP').value) || 1.0
            };
            if (apiKey) {
                dto.apiKey = apiKey;
            } else if (isCopy) {
                dto.apiKey = '__copy__';
                dto.copyFromBotId = p.id;
            }

            const btn = document.getElementById('botSubmitBtn');
            btn.disabled = true;

            if (isEdit) {
                btn.textContent = '保存中...';
                const updated = await BotManager.updateBot(p.id, dto);
                if (updated) {
                    await BotManager.loadGroupBots(this.groupId);
                    await BotManager.loadMyBots();
                    Utils.showToast('机器人已更新', 'success');
                    this._renderListView();
                } else { btn.disabled = false; btn.textContent = submitText; }
            } else {
                btn.textContent = '创建中...';
                const bot = await BotManager.createBot(dto);
                if (bot) {
                    const ok = await BotManager.addBotToGroup(bot.id, this.groupId);
                    await BotManager.loadGroupBots(this.groupId);
                    await BotManager.loadMyBots();
                    if (ok) {
                        Utils.showToast(`机器人 "${bot.name}" 已创建并添加到群聊`, 'success');
                    }
                    this._renderListView();
                } else { btn.disabled = false; btn.textContent = submitText; }
            }
        });
    },

    // ==================== 公共方法 ====================

    _bindCommon(modal) {
        let _mdOnOverlay = false;
        modal.addEventListener('mousedown', (e) => { _mdOnOverlay = e.target === modal; });
        modal.addEventListener('click', (e) => { if (e.target === modal && _mdOnOverlay) Utils.closeModalAnimated(modal); });
        modal.querySelector('.modal-close').addEventListener('click', () => Utils.closeModalAnimated(modal));
    },

    _removeExisting() {
        const existing = document.getElementById('botModalOverlay');
        if (existing) existing.remove();
    }
};
