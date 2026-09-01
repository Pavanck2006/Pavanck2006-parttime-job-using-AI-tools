/**
 * PARTTIME JOB PLATFORM - STUDENT DASHBOARD & ACTIONS
 */

const Student = {
  _profileName: '',

  async loadDashboard() {
    try {
      await this.loadStats();
      await this.loadRecommendedJobs();
      await this.loadAvailableJobs();
      await this.loadApplications();
      await this.loadAcceptedJobs();
      await this.loadCompletedJobs();
      await this.loadProfile();
      await this.loadPayments();
      await this.loadReports();
      await this.loadDeletionRequests();
    } catch (err) {
      console.error('Error loading student dashboard:', err);
    }
  },

  async loadStats() {
    try {
      const stats = await API.student.getDashboardStats();
      document.getElementById('stStatAvailable').innerText = stats.availableJobsCount || 0;
      document.getElementById('stStatApplied').innerText = stats.appliedJobsCount || stats.totalApplicationsCount || 0;
      document.getElementById('stStatAccepted').innerText = stats.acceptedJobsCount || stats.acceptedApplicationsCount || 0;
      document.getElementById('stStatCompleted').innerText = stats.completedJobsCount || 0;
      document.getElementById('stStatEarnings').innerText = '₹' + (stats.totalEarnings || 0).toLocaleString();
    } catch (e) {
      console.warn('Failed to load student stats', e);
    }
  },

    async loadProfile() {
    try {
      this.exitProfileEditMode();
      const profile = await API.student.getProfile();
      document.getElementById('stWelcomeName').innerText = profile.fullName;
      this._profileName = profile.fullName || '';
      document.getElementById('stCollegeBadge').innerText = profile.collegeName || 'Student';
      document.getElementById('stAreaBadge').innerText = profile.preferredArea ? '📍 ' + profile.preferredArea : '📍 Bangalore';
      document.getElementById('stRatingBadge').innerText = '★ ' + (profile.rating || 5.0).toFixed(1);

            // Populate read-only profile view
      document.getElementById('stViewFullName').innerText = profile.fullName || '-';
      document.getElementById('stViewEmail').innerText = profile.email || '-';
      document.getElementById('stViewPhone').innerText = profile.phone || '-';
      document.getElementById('stViewCollege').innerText = profile.collegeName || '-';
      document.getElementById('stViewArea').innerText = profile.preferredArea || '-';
      document.getElementById('stViewRating').innerText = '★ ' + (profile.rating || 5.0).toFixed(1);
      document.getElementById('stViewSkills').innerText = profile.skills || '-';
      document.getElementById('stViewBio').innerText = profile.bio || '-';
      document.getElementById('stViewEmerg').innerText = profile.emergencyContact || '-';
      document.getElementById('stViewJobsDone').innerText = profile.totalJobsCompleted || 0;

      // Profile photo display
      const photoImg = document.getElementById('stViewPhoto');
      if (photoImg) {
        if (profile.profilePhotoUrl) {
          photoImg.src = profile.profilePhotoUrl;
          photoImg.style.display = 'block';
          document.getElementById('stViewPhotoPlaceholder')?.classList.add('d-none');
        } else {
          photoImg.style.display = 'none';
          document.getElementById('stViewPhotoPlaceholder')?.classList.remove('d-none');
        }
      }

      // Populate profile edit form
      document.getElementById('stProfFullName').value = profile.fullName || '';
      document.getElementById('stProfPhone').value = profile.phone || '';
      document.getElementById('stProfCollege').value = profile.collegeName || '';
      document.getElementById('stProfArea').value = profile.preferredArea || '';
      document.getElementById('stProfSkills').value = profile.skills || '';
      document.getElementById('stProfBio').value = profile.bio || '';
      document.getElementById('stProfEmerg').value = profile.emergencyContact || '';
    } catch (e) {
      console.warn('Failed to load student profile', e);
    }
  },

  enterProfileEditMode() {
    document.getElementById('stProfileView')?.classList.add('d-none');
    document.getElementById('stProfEditBtn')?.classList.add('d-none');
    document.getElementById('studentProfileForm')?.classList.remove('d-none');
  },

  exitProfileEditMode() {
    document.getElementById('studentProfileForm')?.classList.add('d-none');
    document.getElementById('stProfileView')?.classList.remove('d-none');
    document.getElementById('stProfEditBtn')?.classList.remove('d-none');
  },

  async updateProfile(e) {
    e.preventDefault();
    const btn = document.getElementById('stProfSubmitBtn');
    try {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';

      let profilePhotoUrl = null;
      const photoFile = document.getElementById('stProfPhoto').files[0];
      if (photoFile) {
        if (photoFile.size > 5 * 1024 * 1024) {
          App.showToast('Photo too large. Max 5MB.', 'warning');
          return;
        }
        const fd = new FormData();
        fd.append('image', photoFile);
        const uploadRes = await fetch('/api/upload/image', {
          method: 'POST',
          headers: {'Authorization': `Bearer ${API.getToken()}`},
          body: fd
        });
        const uploadData = await uploadRes.json();
        if (!uploadRes.ok) throw new Error(uploadData.message || 'Failed to upload photo');
        profilePhotoUrl = uploadData.data?.url;
      }

      const payload = {
        fullName: document.getElementById('stProfFullName').value.trim(),
        phone: document.getElementById('stProfPhone').value.trim(),
        collegeName: document.getElementById('stProfCollege').value.trim(),
        preferredArea: document.getElementById('stProfArea').value.trim(),
        skills: document.getElementById('stProfSkills').value.trim(),
        bio: document.getElementById('stProfBio').value.trim(),
        emergencyContact: document.getElementById('stProfEmerg').value.trim(),
        profilePhotoUrl
      };

            await API.student.updateProfile(payload);
      App.showToast('Profile updated successfully!', 'success');
      await this.loadProfile();
      this.exitProfileEditMode();
    } catch (err) {
      App.showToast(err.message || 'Failed to update profile', 'danger');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-check2-circle me-2"></i>Save Changes';
    }
  },

  async loadRecommendedJobs() {
    const container = document.getElementById('stRecommendedList');
    if (!container) return;

    try {
      container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading recommended jobs...</div>';
      const jobs = await API.public.getRecommendedJobs();

      if (!jobs || jobs.length === 0) {
        container.innerHTML = '<div class="col-12 text-center p-4 text-muted">No specific recommendations found. Browse all available jobs below!</div>';
        return;
      }

      container.innerHTML = jobs.slice(0, 4).map(job => App.renderJobCard(job, 'student')).join('');
    } catch (e) {
      container.innerHTML = '<div class="col-12 text-center text-danger p-3">Failed to load recommendations</div>';
    }
  },

  async loadApplications() {
    const container = document.getElementById('stApplicationsList');
    if (!container) return;

    try {
      container.innerHTML = '<tr><td colspan="7" class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading applications...</td></tr>';
      const apps = await API.student.getMyApplications();

      if (!apps || apps.length === 0) {
        container.innerHTML = '<tr><td colspan="7" class="text-center p-4 text-muted">You have not applied for any jobs yet. Browse available jobs and apply!</td></tr>';
        return;
      }

      container.innerHTML = apps.map(app => {
        let statusBadge = `<span class="badge bg-secondary">${app.status}</span>`;
        if (app.status === 'ACCEPTED') statusBadge = `<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Accepted</span>`;
        if (app.status === 'REJECTED') statusBadge = `<span class="badge bg-danger">Not Selected</span>`;
        if (app.status === 'CANCELLED') statusBadge = `<span class="badge bg-warning text-dark">Cancelled</span>`;
        if (app.status === 'COMPLETED') statusBadge = `<span class="badge bg-primary">Completed</span>`;

        let actionBtn = '';
        if (app.status === 'APPLIED') {
          actionBtn = `<button class="btn btn-sm btn-outline-danger" onclick="Student.cancelApplication(${app.id})"><i class="bi bi-x-circle me-1"></i>Cancel</button>`;
        } else if (app.status === 'ACCEPTED') {
          actionBtn = `<button class="btn btn-sm btn-success" onclick="Student.viewAcceptedJob(${app.id})"><i class="bi bi-geo-alt me-1"></i>View Details</button>`;
        } else {
          actionBtn = `<button class="btn btn-sm btn-outline-secondary" onclick="App.viewJobDetails(${app.jobId})">View Job</button>`;
        }

        return `
          <tr>
            <td>
              <div class="fw-bold">${App.escapeHtml(app.jobTitle || 'Catering Job')}</div>
              <small class="text-muted">${App.escapeHtml(app.cateringName || '')} ${app.ownerVerified ? '<i class="bi bi-patch-check-fill text-success"></i>' : ''}</small>
            </td>
            <td><span class="badge bg-primary-subtle text-primary border">${app.workTypeDisplayName || app.workType}</span></td>
            <td>📍 ${App.escapeHtml(app.workArea || '')}</td>
            <td>
              <div>${App.formatDate(app.jobDate)}</div>
              <small class="text-muted">${App.formatTime(app.startTime)} - ${App.formatTime(app.endTime)}</small>
            </td>
            <td class="fw-bold text-success">₹${app.paymentAmount}</td>
            <td>${statusBadge}</td>
            <td>${actionBtn}</td>
          </tr>
        `;
      }).join('');
    } catch (err) {
      container.innerHTML = `<tr><td colspan="7" class="text-center p-3 text-danger">Failed to load applications: ${err.message}</td></tr>`;
    }
  },

  async loadAcceptedJobs() {
    const container = document.getElementById('stAcceptedList');
    if (!container) return;

    try {
      container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading accepted shifts...</div>';
      const apps = await API.student.getAcceptedJobs();

      if (!apps || apps.length === 0) {
        container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><i class="bi bi-calendar2-check fs-2 d-block mb-2"></i>No accepted shifts pending. Apply for jobs to get hired!</div>';
        return;
      }

      container.innerHTML = apps.map(app => `
        <div class="col-md-6 mb-3">
          <div class="card h-100 border-success shadow-sm" style="border-left: 5px solid #10b981 !important;">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <span class="badge bg-success-subtle text-success border border-success-subtle fw-bold"><i class="bi bi-shield-check me-1"></i>Shift Confirmed</span>
                <span class="fw-bold fs-5 text-success">₹${app.paymentAmount} <small class="fs-6 text-muted">(${app.paymentTypeDisplayName || 'On-Spot'})</small></span>
              </div>
              <h5 class="card-title fw-bold mb-1">${App.escapeHtml(app.jobTitle)}</h5>
              <div class="text-muted mb-3"><i class="bi bi-building me-1"></i>${App.escapeHtml(app.cateringName)} ${app.ownerVerified ? '<i class="bi bi-patch-check-fill text-success" title="Verified Catering"></i>' : ''}</div>
              
              <div class="sensitive-box-unlocked mb-3">
                <div class="fw-bold mb-1"><i class="bi bi-geo-alt-fill text-danger me-1"></i>Exact Work Address:</div>
                <div class="mb-2 text-dark">${App.escapeHtml(app.detailedLocation || 'Address unlocked')}</div>
                <div class="d-flex flex-wrap gap-3 pt-2 border-top border-success-subtle">
                  <div><i class="bi bi-telephone-fill text-primary me-1"></i><strong>Phone:</strong> <a href="tel:${app.contactPhone}">${app.contactPhone}</a></div>
                  ${app.contactEmail ? `<div><i class="bi bi-envelope-fill text-primary me-1"></i><strong>Email:</strong> ${app.contactEmail}</div>` : ''}
                </div>
              </div>

              <div class="row g-2 mb-3 text-muted small">
                <div class="col-6"><i class="bi bi-calendar-event me-1"></i>${App.formatDate(app.jobDate)}</div>
                <div class="col-6"><i class="bi bi-clock me-1"></i>${App.formatTime(app.startTime)} - ${App.formatTime(app.endTime)}</div>
                <div class="col-6"><i class="bi bi-person-badge me-1"></i>Organizer: ${App.escapeHtml(app.ownerName || 'Catering Owner')}</div>
                <div class="col-6"><i class="bi bi-check2-circle me-1"></i>Attendance: <strong>${app.attendanceStatus}</strong></div>
              </div>

              <div class="d-flex gap-2 flex-wrap">
                <button class="btn btn-sm btn-success flex-grow-1" onclick="Student.openRazorpayPayModal(${app.id}, '${App.escapeHtml(app.jobTitle)}', '${App.escapeHtml(app.ownerName || app.cateringName)}', ${app.paymentAmount})">
                  <i class="bi bi-credit-card me-1"></i>Pay Online
                </button>
                <button class="btn btn-sm btn-outline-success" onclick="App.openConfirmPaymentModal(${app.id}, '${App.escapeHtml(app.jobTitle)}', ${app.paymentAmount})">
                  <i class="bi bi-cash-stack me-1"></i>On-Spot
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="App.openDisputeModal(${app.jobId}, ${app.id}, '${App.escapeHtml(app.jobTitle)}', ${app.paymentAmount})">
                  <i class="bi bi-exclamation-triangle me-1"></i>Dispute
                </button>
              </div>
            </div>
          </div>
        </div>
      `).join('');
    } catch (err) {
      container.innerHTML = `<div class="col-12 text-center text-danger p-3">Failed to load accepted shifts: ${err.message}</div>`;
    }
  },

  async loadCompletedJobs() {
    const container = document.getElementById('stCompletedList');
    if (!container) return;

    try {
      container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading completed jobs...</td></tr>';
      const apps = await API.student.getCompletedJobs();

      if (!apps || apps.length === 0) {
        container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted">No completed jobs yet.</td></tr>';
        return;
      }

      container.innerHTML = apps.map(app => `
        <tr>
          <td class="fw-bold">${App.escapeHtml(app.jobTitle)}</td>
          <td>${App.escapeHtml(app.cateringName)}</td>
          <td>${App.formatDate(app.jobDate)}</td>
          <td class="text-success fw-bold">₹${app.paymentAmount}</td>
          <td>
            ${app.paymentStatus === 'CONFIRMED' ? '<span class="badge bg-success"><i class="bi bi-check-all me-1"></i>Confirmed</span>' :
              app.paymentStatus === 'PAID' ? '<span class="badge bg-info text-dark">Paid (Pending Confirm)</span>' :
              app.paymentStatus === 'DISPUTED' ? '<span class="badge bg-danger">Disputed</span>' :
              '<span class="badge bg-warning text-dark">Pending</span>'}
          </td>
          <td>
            ${app.paymentStatus !== 'CONFIRMED' ? `
              <button class="btn btn-sm btn-outline-success me-1" onclick="App.openConfirmPaymentModal(${app.id}, '${App.escapeHtml(app.jobTitle)}', ${app.paymentAmount})">Confirm</button>
              <button class="btn btn-sm btn-outline-danger" onclick="App.openDisputeModal(${app.jobId}, ${app.id}, '${App.escapeHtml(app.jobTitle)}', ${app.paymentAmount})">Dispute</button>
            ` : '<span class="text-muted small"><i class="bi bi-check-circle text-success me-1"></i>Settled</span>'}
          </td>
        </tr>
      `).join('');
    } catch (err) {
      container.innerHTML = `<tr><td colspan="6" class="text-center p-3 text-danger">Failed to load completed jobs</td></tr>`;
    }
  },

  async loadReports() {
    const container = document.getElementById('stReportsList');
    if (!container) return;

    try {
      const reports = await API.reports.getMyReports();
      if (!reports || reports.length === 0) {
        container.innerHTML = '<div class="text-center p-4 text-muted">You have not submitted any complaints.</div>';
        return;
      }
      container.innerHTML = reports.map(report => `
        <article class="card border mb-3 shadow-sm">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
              <div><span class="badge bg-danger-subtle text-danger">${App.escapeHtml(report.report_type || 'Complaint')}</span><h6 class="fw-bold mt-2 mb-0">${App.escapeHtml(report.description || 'Complaint')}</h6></div>
              <span class="badge ${report.status === 'RESOLVED' ? 'bg-success' : 'bg-warning text-dark'}">${App.escapeHtml(report.status || 'PENDING')}</span>
            </div>
            <div class="small text-muted mb-3">Expected: ₹${App.escapeHtml(report.expected_amount ?? 'Not provided')} &middot; Received: ₹${App.escapeHtml(report.received_amount ?? '0')} &middot; Submitted: ${App.formatDate(report.created_at)}</div>
            ${report.admin_remarks ? `<div class="alert alert-info py-2 small mb-3"><strong>Admin response:</strong> ${App.escapeHtml(report.admin_remarks)}</div>` : ''}
            <button class="btn btn-outline-danger btn-sm" type="button" onclick="Student.withdrawReport(${report.id})"><i class="bi bi-arrow-counterclockwise me-1"></i>Withdraw Complaint</button>
          </div>
        </article>
      `).join('');
    } catch (err) {
      container.innerHTML = `<div class="text-center text-danger p-3">Failed to load complaints: ${App.escapeHtml(err.message)}</div>`;
    }
  },

  async withdrawReport(reportId) {
    if (!confirm('Withdraw this complaint? It will be removed from the owner and admin lists.')) return;
    try {
      await API.reports.withdraw(reportId);
      App.showToast('Complaint withdrawn successfully.', 'success');
      await this.loadReports();
    } catch (err) {
      App.showToast(err.message || 'Failed to withdraw complaint', 'danger');
    }
  },



  async loadDeletionRequests() {
    const container = document.getElementById('stDeletionRequestsList');
    if (!container) return;
    try {
      container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading deletion requests...</div>';
      const requests = await API.student.getDeletionRequests();
      if (!requests || requests.length === 0) {
        container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><i class="bi bi-check-circle fs-2 d-block mb-2"></i>No pending job deletion requests.</div>';
        return;
      }
      container.innerHTML = requests.map(req => {
        let statusBadge = '';
        if (req.status === 'PENDING') statusBadge = '<span class="badge bg-warning text-dark">Awaiting Your Response</span>';
        else if (req.status === 'ACCEPTED') statusBadge = '<span class="badge bg-success">Accepted — Job Deleted</span>';
        else if (req.status === 'REJECTED') statusBadge = '<span class="badge bg-info">Rejected — Job Active</span>';
        return `
          <div class="col-md-6 mb-3">
            <div class="card h-100 shadow-sm ${req.status === 'PENDING' ? 'border-warning' : req.status === 'ACCEPTED' ? 'border-success' : 'border-secondary'}">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-2">
                  <h5 class="fw-bold mb-0">${App.escapeHtml(req.jobTitle)}</h5>
                  ${statusBadge}
                </div>
                <div class="text-muted small mb-2">
                  <i class="bi bi-building me-1"></i>${App.escapeHtml(req.cateringName || req.ownerName)}
                  <span class="ms-2">📅 ${App.formatDate(req.jobDate)}</span>
                </div>
                <div class="text-muted small mb-3">
                  <i class="bi bi-clock me-1"></i>${App.formatTime(req.startTime)} - ${App.formatTime(req.endTime)}
                  <span class="ms-2">📍 ${App.escapeHtml(req.workArea)}</span>
                </div>
                ${req.status === 'PENDING' ? `
                  <div class="alert alert-warning py-2 mb-3 small">
                    <i class="bi bi-exclamation-triangle me-1"></i>The owner wants to delete this job. If you accept, the job will be permanently removed.
                  </div>
                  <div class="d-flex gap-2">
                    <button class="btn btn-success flex-grow-1" onclick="Student.handleDeletionRequest(${req.id}, 'accept')">
                      <i class="bi bi-check-circle me-1"></i>Accept — Delete Job
                    </button>
                    <button class="btn btn-outline-danger flex-grow-1" onclick="Student.handleDeletionRequest(${req.id}, 'reject')">
                      <i class="bi bi-x-circle me-1"></i>Reject — Keep Job
                    </button>
                  </div>
                ` : req.status === 'ACCEPTED' ? `
                  <div class="alert alert-success py-2 mb-0 small">Job has been deleted as per your approval.</div>
                ` : `
                  <div class="alert alert-secondary py-2 mb-0 small">You rejected the deletion. The job remains active.</div>
                `}
              </div>
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      container.innerHTML = `<div class="col-12 text-center text-danger p-3">Failed to load deletion requests: ${err.message}</div>`;
    }
  },

  async handleDeletionRequest(requestId, action) {
    const msg = action === 'accept'
      ? 'Accept this deletion request? The job will be permanently deleted.'
      : 'Reject this deletion request? The job will remain active.';
    if (!confirm(msg)) return;
    try {
      if (action === 'accept') {
        await API.student.acceptDeletionRequest(requestId);
        App.showToast('Request accepted. The job has been deleted.', 'success');
      } else {
        await API.student.rejectDeletionRequest(requestId);
        App.showToast('Request rejected. The job remains active.', 'info');
      }
      await this.loadDeletionRequests();
      await this.loadApplications();
    } catch (err) {
      App.showToast(err.message || 'Failed to process request', 'danger');
    }
  },

  async loadPayments() {
    const container = document.getElementById('stPaymentsList');
    if (!container) return;

    try {
      container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading payments...</td></tr>';
      const payments = await API.student.getPayments();

      if (!payments || payments.length === 0) {
        container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted">No payment records found yet.</td></tr>';
        return;
      }

      let rpMap = {};
      try { const rps = await Student.loadRazorpayTransactions(); rps.forEach(t => { rpMap[t.applicationId] = t; }); } catch(e) {}
      container.innerHTML = payments.map(pay => { const rp = rpMap[pay.applicationId] || {}; const isOnline = !!(rp.razorpayPaymentId || rp.paymentMethod); return `
        <tr>
          <td>
            <div class="fw-bold">${App.escapeHtml(pay.jobTitle)}</div>
            <small class="text-muted">${App.escapeHtml(pay.workArea || '')}</small>
          </td>
          <td>
            <div class="fw-semibold">${App.escapeHtml(this._profileName)}</div>
            <small class="text-muted">${App.escapeHtml(pay.workArea || '')}</small>
          </td>
          <td class="fw-bold text-success">₹${pay.amount}</td>
          <td><span class="badge bg-primary-subtle text-primary border">${pay.paymentTypeDisplayName || 'On-Spot'}</span>${isOnline ? '<br><span class="badge bg-info text-dark mt-1"><i class="bi bi-credit-card me-1"></i>Online</span>' : ''}</td>
          <td>
            ${rp.paymentStatus === 'SUCCESS' || rp.paymentStatus === 'CONFIRMED' ? '<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Paid</span>' :
              rp.paymentStatus === 'CREATED' ? '<span class="badge bg-warning text-dark">Processing</span>' :
              rp.paymentStatus === 'FAILED' ? '<span class="badge bg-danger">Failed</span>' :
              pay.paymentStatus === 'CONFIRMED' ? '<span class="badge bg-success">Confirmed</span>' :
              pay.paymentStatus === 'PAID' ? '<span class="badge bg-info">Marked Paid</span>' :
              pay.paymentStatus === 'DISPUTED' ? '<span class="badge bg-danger">Disputed</span>' :
              '<span class="badge bg-warning text-dark">Pending</span>'}
            ${rp.razorpayPaymentId ? `<br><small class="text-muted">RP: ${rp.razorpayPaymentId.substring(0, 20)}</small>` : ''}
            ${rp.environment ? '<br><span class="badge bg-warning-subtle text-warning border border-warning-subtle small mt-1"><i class="bi bi-info-circle me-1"></i>TEST</span>' : ''}
          </td>
          <td><small class="text-muted">${App.formatDate(pay.confirmedPaidAt || pay.markedPaidAt || pay.createdAt)}</small></td>
        </tr>
      `}).join('');
    } catch (err) {
      container.innerHTML = `<tr><td colspan="6" class="text-center p-3 text-danger">Failed to load payment history</td></tr>`;
    }
  },

  async cancelApplication(id) {
    if (!confirm('Are you sure you want to cancel this application?')) return;
    try {
      await API.student.cancelApplication(id);
      App.showToast('Application cancelled successfully', 'info');
      await this.loadApplications();
      await this.loadStats();
    } catch (err) {
      App.showToast(err.message || 'Failed to cancel application', 'danger');
    }
  },

  viewAcceptedJob(appId) {
    // Switch to accepted tab
    const tabEl = document.querySelector('button[data-bs-target="#st-tab-accepted"]');
    if (tabEl) {
      const tab = new bootstrap.Tab(tabEl);
      tab.show();
    }
  },

  async loadAvailableJobs() {
    const container = document.getElementById('stAvailableJobsList');
    if (!container) return;

    try {
      container.innerHTML = '<div class="col-12 text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading available jobs...</div>';
      const jobs = await API.public.getRecommendedJobs();
      this._allAvailableJobs = jobs || [];

      if (!jobs || jobs.length === 0) {
        container.innerHTML = '<div class="col-12 text-center p-4 text-muted">No jobs available at the moment. Check back later!</div>';
        return;
      }

      this._renderAvailableJobs(jobs);
    } catch (e) {
      container.innerHTML = '<div class="col-12 text-center text-danger p-3">Failed to load available jobs</div>';
    }
  },

  _renderAvailableJobs(jobs) {
    const container = document.getElementById('stAvailableJobsList');
    if (!container) return;

    if (!jobs || jobs.length === 0) {
      container.innerHTML = '<div class="col-12 text-center p-4 text-muted">No jobs match your search.</div>';
      return;
    }

    container.innerHTML = jobs.map(job => App.renderJobCard(job, 'student')).join('');
  },

  filterAvailableJobs() {
    const searchInput = document.getElementById('stAvailableSearch');
    const query = (searchInput?.value || '').trim().toLowerCase();
    const allJobs = this._allAvailableJobs || [];

    if (!query) {
      this._renderAvailableJobs(allJobs);
      return;
    }

    const filtered = allJobs.filter(job => {
      const area = (job.workArea || '').toLowerCase();
      const title = (job.title || '').toLowerCase();
      const catering = (job.cateringName || '').toLowerCase();
      return area.includes(query) || title.includes(query) || catering.includes(query);
    });

    this._renderAvailableJobs(filtered);
  },

  // ─── RAZORPAY PAYMENT ──────────────────────────────────────────────────

  async openRazorpayPayModal(paymentRecordId, jobTitle, ownerName, amount) {
    document.getElementById('rpPayRecordId').value = paymentRecordId;
    document.getElementById('rpPayJobTitle').textContent = jobTitle;
    document.getElementById('rpPayOwner').textContent = ownerName;
    document.getElementById('rpPayAmount').textContent = '\u20B9' + Number(amount).toLocaleString('en-IN');
    document.getElementById('rpPayStatus').style.display = 'none';
    document.getElementById('rpPayNowBtn').style.display = '';
    document.getElementById('rpPayNowBtn').disabled = false;
    new bootstrap.Modal(document.getElementById('razorpayPayModal')).show();
  },

  async initiateRazorpayPayment() {
    const btn = document.getElementById('rpPayNowBtn');
    const status = document.getElementById('rpPayStatus');
    const recordId = document.getElementById('rpPayRecordId').value;
    if (!recordId) return;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating order...';
    status.style.display = 'none';

    try {
      // 1. Create order on server
      const res = await fetch('/api/student/razorpay/create-order', {
        method: 'POST',
        headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + API.getToken()},
        body: JSON.stringify({paymentRecordId: Number(recordId)})
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || 'Failed to create order');

      // 2. Fetch Razorpay key
      const keyRes = await fetch('/api/public/razorpay-key', {
        headers: {'Authorization': 'Bearer ' + API.getToken()}
      });
      const keyData = await keyRes.json();
      if (!keyData.data || !keyData.data.keyId) throw new Error('Razorpay is not configured');

      // 3. Open Razorpay Checkout
      const options = {
        key: keyData.data.keyId,
        amount: data.data.amount,
        currency: data.data.currency || 'INR',
        name: 'PartTime Job Platform',
        description: 'Payment for: ' + (data.data.jobTitle || 'Job'),
        order_id: data.data.orderId,
        handler: async function(response) {
          // 4. Verify payment on server
          status.style.display = '';
          btn.style.display = 'none';
          try {
            const verifyRes = await fetch('/api/student/razorpay/verify', {
              method: 'POST',
              headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + API.getToken()},
              body: JSON.stringify({
                razorpay_order_id: response.razorpay_order_id,
                razorpay_payment_id: response.razorpay_payment_id,
                razorpay_signature: response.razorpay_signature,
                paymentRecordId: Number(recordId)
              })
            });
            const verifyData = await verifyRes.json();
            if (!verifyRes.ok) throw new Error(verifyData.message || 'Verification failed');

            // Success!
            bootstrap.Modal.getInstance(document.getElementById('razorpayPayModal')).hide();
            document.getElementById('rpSuccessDetails').innerHTML =
              '<div>Payment ID: <code>' + response.razorpay_payment_id + '</code></div>' +
              '<div>Amount: <strong>\u20B9' + (data.data.amount / 100).toLocaleString('en-IN') + '</strong></div>';
            new bootstrap.Modal(document.getElementById('razorpaySuccessModal')).show();

            // Refresh the page data
            if (typeof Student !== 'undefined' && Student.loadAcceptedJobs) Student.loadAcceptedJobs();
            if (typeof Student !== 'undefined' && Student.loadPayments) Student.loadPayments();
            App.toast('Payment successful!', 'success');
          } catch (e) {
            App.toast('Payment succeeded but verification failed: ' + e.message + '. Contact support.', 'warning');
          }
        },
        prefill: {},
        theme: {color: '#10b981'},
        modal: {
          ondismiss: async function() {
            // User cancelled - mark as cancelled on server
            btn.style.display = '';
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-lock-fill me-2"></i>Pay Now';
            status.style.display = 'none';
            try {
              await fetch('/api/student/razorpay/cancel', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + API.getToken()},
                body: JSON.stringify({paymentRecordId: Number(recordId)})
              });
            } catch (e) { /* ignore */ }
          }
        }
      };

      const rzp = new Razorpay(options);
      rzp.on('payment.failed', function(response) {
        App.toast('Payment failed: ' + (response.error ? response.error.description : 'Unknown error'), 'danger');
        btn.style.display = '';
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-lock-fill me-2"></i>Pay Now';
        status.style.display = 'none';
      });
      rzp.open();

    } catch (e) {
      App.toast(e.message, 'danger');
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-lock-fill me-2"></i>Pay Now';
    }
  },

  async loadRazorpayTransactions() {
    try {
      const res = await fetch('/api/student/razorpay/transactions', {
        headers: {'Authorization': 'Bearer ' + API.getToken()}
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message);
      return data.data || [];
    } catch (e) {
      console.error('Failed to load transactions:', e);
      return [];
    }
  }
};
