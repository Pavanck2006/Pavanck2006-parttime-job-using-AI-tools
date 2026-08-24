const Chat = {
  async init() {
    await this.loadReports('student');
    await this.loadReports('owner');
  },

  async loadReports(role) {
    const select = document.getElementById(role === 'student' ? 'studentChatReport' : 'ownerChatReport');
    if (!select) return;
    try {
      const reports = role === 'student' ? await API.reports.getMyReports() : await API.owner.getComplaints();
      select.innerHTML = '<option value="">Choose a complaint</option>' + (reports || []).map(report => `<option value="${report.id}">${App.escapeHtml(report.report_type || report.reportType || 'Complaint')} - ${App.escapeHtml(report.description || 'Complaint').slice(0, 55)}</option>`).join('');
    } catch (error) {
      select.innerHTML = '<option value="">Unable to load complaints</option>';
    }
  },

  async selectReport(role) {
    const select = document.getElementById(role === 'student' ? 'studentChatReport' : 'ownerChatReport');
    const reportId = select?.value;
    const messages = document.getElementById(role === 'student' ? 'studentChatMessages' : 'ownerChatMessages');
    if (!messages) return;
    if (!reportId) {
      messages.innerHTML = '<div class="text-center text-muted p-4">Choose a complaint to start or continue the discussion.</div>';
      return;
    }
    try {
      const items = await API.chat.getMessages(reportId);
      messages.innerHTML = !items?.length ? '<div class="chat-empty"><i class="bi bi-chat-square-text"></i><span>No messages yet. Send the first message.</span></div>' : items.map(item => { const mine = item.senderId === API.getCurrentUser()?.id; return `<div class="chat-message ${mine ? 'mine' : 'theirs'}"><div class="chat-bubble"><div class="chat-sender">${App.escapeHtml(item.senderName)}</div><div>${App.escapeHtml(item.message)}</div></div><div class="chat-time">${App.formatTimeAgo(item.createdAt)}</div></div>`; }).join('');
      messages.scrollTop = messages.scrollHeight;
    } catch (error) {
      messages.innerHTML = `<div class="text-center text-danger p-3">${App.escapeHtml(error.message)}</div>`;
    }
  },

  async send(role) {
    const prefix = role === 'student' ? 'student' : 'owner';
    const reportId = document.getElementById(`${prefix}ChatReport`)?.value;
    const input = document.getElementById(`${prefix}ChatInput`);
    if (!reportId || !input?.value.trim()) return;
    try {
      await API.chat.sendMessage(reportId, input.value.trim());
      input.value = '';
      await this.selectReport(role);
    } catch (error) {
      App.showToast(error.message || 'Unable to send message', 'danger');
    }
  }
};
