/**
 * PARTTIME JOB PLATFORM - OWNER DASHBOARD & ACTIONS
 */

const Owner = {
  currentActiveJobId: null,

  async loadDashboard() {
    try {
      await this.loadProfile();
      await this.loadMyJobs();
      await this.loadPayments();
      await this.loadComplaints();
    } catch (err) {
      console.error('Error loading owner dashboard:', err);
    }
  },

  async loadProfile() {
    try {
      const profile = await API.owner.getProfile();
      document.getElementById('owWelcomeCatering').innerText = profile.cateringName;
      document.getElementById('owOwnerName').innerText = profile.fullName;
      
      const badgeEl = document.getElementById('owVerificationBadge');
      if (profile.verified) {
        badgeEl.className = 'verified-badge';
        badgeEl.innerHTML = '<i class="bi bi-patch-check-fill"></i> Verified Catering Business';
        document.getElementById('owUnverifiedAlert')?.classList.add('d-none');
      } else {
        badgeEl.className = 'unverified-badge';
        badgeEl.innerHTML = '<i class="bi bi-hourglass-split"></i> Pending Admin Verification';
        document.getElementById('owUnverifiedAlert')?.classList.remove('d-none');
      }

      // Populate profile edit form
      document.getElementById('owProfFullName').value = profile.fullName || '';
      document.getElementById('owProfPhone').value = profile.phone || '';
      document.getElementById('owProfCateringName').value = profile.cateringName || '';
      document.getElementById('owProfAddress').value = profile.businessAddress || '';
      document.getElementById('owProfBizPhone').value = profile.businessPhone || '';
    } catch (e) {
      console.warn('Failed to load owner profile', e);
    }
  },

  async updateProfile(e) {
    e.preventDefault();
    const btn = document.getElementById('owProfSubmitBtn');
    try {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Saving...';

      const payload = {
        fullName: document.getElementById('owProfFullName').value.trim(),
        phone: document.getElementById('owProfPhone').value.trim(),
        cateringName: document.getElementById('owProfCateringName').value.trim(),
        businessAddress: document.getElementById('owProfAddress').value.trim(),
        businessPhone: document.getElementById('owProfBizPhone').value.trim()
      };

      await API.owner.updateProfile(payload);
      App.showToast('Catering profile updated successfully!', 'success');
      await this.loadProfile();
    } catch (err) {
      App.showToast(err.message || 'Failed to update profile', 'danger');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-check2-circle me-2"></i>Save Changes';
    }
  },

  async loadMyJobs() {
    const container = document.getElementById('owJobsList');
    if (!container) return;

    try {
      container.innerHTML = '<div class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading your jobs...</div>';
      const jobs = await API.owner.getMyJobs();

      // Update owner metrics
      let activeJobsCount = 0;
      let completedJobsCount = 0;
      let totalHired = 0;

      if (jobs && jobs.length > 0) {
        jobs.forEach(j => {
          if (j.status === 'OPEN' || j.status === 'FILLED') activeJobsCount++;
          if (j.status === 'COMPLETED') completedJobsCount++;
          totalHired += (j.workersSelected || 0);
        });
      }

      document.getElementById('owStatTotalJobs').innerText = jobs?.length || 0;
      document.getElementById('owStatActiveJobs').innerText = activeJobsCount;
      document.getElementById('owStatHired').innerText = totalHired;
      document.getElementById('owStatCompleted').innerText = completedJobsCount;

      if (!jobs || jobs.length === 0) {
        container.innerHTML = '<div class="text-center p-4 text-muted"><i class="bi bi-briefcase fs-2 d-block mb-2"></i>You have not posted any catering jobs yet. Click "Create New Job" to post!</div>';
        return;
      }

      container.innerHTML = jobs.map(job => {
        let statusBadge = `<span class="badge bg-success">OPEN</span>`;
        if (job.status === 'FILLED') statusBadge = `<span class="badge bg-warning text-dark">SLOTS FILLED (${job.workersSelected}/${job.workersRequired})</span>`;
        if (job.status === 'COMPLETED') statusBadge = `<span class="badge bg-primary">COMPLETED</span>`;
        if (job.status === 'CANCELLED') statusBadge = `<span class="badge bg-secondary">CANCELLED</span>`;

        const isEditable = job.status === 'OPEN' || job.status === 'FILLED';

        return `
          <div class="card mb-3 shadow-sm border-0 border-start border-4 ${job.status === 'COMPLETED' ? 'border-primary' : job.status === 'CANCELLED' ? 'border-secondary' : 'border-success'}">
            <div class="card-body">
              <div class="row align-items-center">
                <div class="col-lg-5 mb-2 mb-lg-0">
                  <div class="d-flex align-items-center gap-2 mb-1">
                    <h5 class="fw-bold mb-0">${App.escapeHtml(job.title)}</h5>
                    ${statusBadge}
                  </div>
                  <div class="text-muted small mb-2">
                    <span class="badge bg-light text-dark border me-1">${job.workTypeDisplayName || job.workType}</span>
                    <span>📍 ${App.escapeHtml(job.workArea)}</span>
                    <span class="ms-2">📅 ${App.formatDate(job.jobDate)} (${App.formatTime(job.startTime)} - ${App.formatTime(job.endTime)})</span>
                  </div>
                  <div class="small text-muted"><i class="bi bi-geo-alt me-1"></i>${App.escapeHtml(job.detailedLocation)}</div>
                </div>

                <div class="col-lg-3 mb-2 mb-lg-0 text-lg-center">
                  <div class="fw-bold fs-5 text-success">₹${job.paymentAmount}</div>
                  <span class="badge ${job.onSpotPayment ? 'bg-success-subtle text-success' : 'bg-light text-dark'} border">${job.paymentTypeDisplayName || 'On-Spot'}</span>
                  <div class="small text-muted mt-1">Hired: <strong>${job.workersSelected}</strong> / ${job.workersRequired} workers</div>
                </div>

                <div class="col-lg-4 text-lg-end d-flex flex-wrap gap-1 justify-content-lg-end">
                  <button class="btn btn-sm btn-primary-custom" onclick="Owner.openApplicantsModal(${job.id}, '${App.escapeHtml(job.title)}')">
                    <i class="bi bi-people-fill me-1"></i>Applicants (${job.applications?.length || 0})
                  </button>
                  
                  ${isEditable ? `
                    <button class="btn btn-sm btn-outline-success" onclick="Owner.completeJob(${job.id})">
                      <i class="bi bi-check2-all me-1"></i>Complete
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="Owner.cancelJob(${job.id})">
                      <i class="bi bi-trash"></i>
                    </button>
                  ` : ''}
                </div>
              </div>
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      container.innerHTML = `<div class="text-center text-danger p-3">Failed to load jobs: ${err.message}</div>`;
    }
  },

  async handleCreateJob(e) {
    e.preventDefault();
    const btn = document.getElementById('createJobSubmitBtn');
    try {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Posting Job...';

      const payload = {
        title: document.getElementById('jobTitle').value.trim(),
        description: document.getElementById('jobDescription').value.trim(),
        workType: document.getElementById('jobWorkType').value,
        workArea: document.getElementById('jobWorkArea').value.trim(),
        detailedLocation: document.getElementById('jobDetailedLocation').value.trim(),
        jobDate: document.getElementById('jobDate').value,
        startTime: document.getElementById('jobStartTime').value,
        endTime: document.getElementById('jobEndTime').value,
        paymentAmount: parseFloat(document.getElementById('jobPaymentAmount').value),
        paymentType: document.getElementById('jobPaymentType').value,
        onSpotPayment: document.getElementById('jobOnSpotCheckbox').checked,
        workersRequired: parseInt(document.getElementById('jobWorkersRequired').value, 10),
        requiredSkills: document.getElementById('jobRequiredSkills').value.trim(),
        contactPhone: document.getElementById('jobContactPhone').value.trim(),
        contactEmail: document.getElementById('jobContactEmail').value.trim()
      };

      await API.owner.createJob(payload);
      App.showToast('Job posted successfully!', 'success');

      // Reset form
      document.getElementById('createJobForm').reset();
      document.getElementById('jobOnSpotCheckbox').checked = true;

      // Switch to Active Jobs tab
      const tabEl = document.querySelector('button[data-bs-target="#ow-tab-jobs"]');
      if (tabEl) {
        const tab = new bootstrap.Tab(tabEl);
        tab.show();
      }

      await this.loadMyJobs();
    } catch (err) {
      App.showToast(err.message || 'Failed to post job', 'danger');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-plus-circle me-2"></i>Post Catering Job';
    }
  },

  async openApplicantsModal(jobId, jobTitle) {
    this.currentActiveJobId = jobId;
    document.getElementById('applicantsJobTitle').innerText = jobTitle;
    const container = document.getElementById('applicantsListContent');
    const modal = new bootstrap.Modal(document.getElementById('applicantsModal'));
    modal.show();

    try {
      container.innerHTML = '<div class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading applicants...</div>';
      const apps = await API.owner.getJobApplications(jobId);

      if (!apps || apps.length === 0) {
        container.innerHTML = '<div class="text-center p-4 text-muted">No students have applied for this job yet.</div>';
        return;
      }

      container.innerHTML = `
        <div class="table-responsive">
          <table class="table align-middle">
            <thead>
              <tr>
                <th>Student</th>
                <th>College / Skills</th>
                <th>Applied At</th>
                <th>Status</th>
                <th>Attendance</th>
                <th>Payment</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              ${apps.map(app => `
                <tr>
                  <td>
                    <div class="fw-bold">${App.escapeHtml(app.studentName)}</div>
                    <small class="text-muted"><i class="bi bi-star-fill text-warning me-1"></i>${app.studentRating || 5.0} (${app.totalJobsCompleted || 0} completed)</small>
                    ${app.status === 'ACCEPTED' || app.status === 'COMPLETED' ? `<div class="small text-primary"><a href="tel:${app.studentPhone}"><i class="bi bi-telephone-fill me-1"></i>${app.studentPhone}</a></div>` : ''}
                  </td>
                  <td>
                    <div class="small fw-semibold">${App.escapeHtml(app.collegeName || 'Student')}</div>
                    <div class="small text-muted">${App.escapeHtml(app.skills || 'General')}</div>
                    ${app.notes ? `<div class="small text-info mt-1"><em>Note: "${App.escapeHtml(app.notes)}"</em></div>` : ''}
                  </td>
                  <td class="small text-muted">${App.formatTimeAgo(app.appliedAt)}</td>
                  <td>
                    ${app.status === 'ACCEPTED' ? '<span class="badge bg-success">Accepted</span>' :
                      app.status === 'REJECTED' ? '<span class="badge bg-danger">Rejected</span>' :
                      app.status === 'CANCELLED' ? '<span class="badge bg-secondary">Cancelled</span>' :
                      app.status === 'COMPLETED' ? '<span class="badge bg-primary">Completed</span>' :
                      '<span class="badge bg-warning text-dark">Pending</span>'}
                  </td>
                  <td>
                    <select class="form-select form-select-sm" style="width: 120px;" onchange="Owner.updateAttendance(${app.id}, this.value)" ${app.status !== 'ACCEPTED' && app.status !== 'COMPLETED' ? 'disabled' : ''}>
                      <option value="NOT_MARKED" ${app.attendanceStatus === 'NOT_MARKED' ? 'selected' : ''}>Not Marked</option>
                      <option value="PRESENT" ${app.attendanceStatus === 'PRESENT' ? 'selected' : ''}>Present</option>
                      <option value="ABSENT" ${app.attendanceStatus === 'ABSENT' ? 'selected' : ''}>Absent</option>
                    </select>
                  </td>
                  <td>
                    ${app.paymentStatus === 'CONFIRMED' ? '<span class="badge bg-success"><i class="bi bi-check-all me-1"></i>Confirmed</span>' :
                      app.paymentStatus === 'PAID' ? '<span class="badge bg-info text-dark">Marked Paid</span>' :
                      app.paymentStatus === 'DISPUTED' ? '<span class="badge bg-danger">Disputed</span>' :
                      '<span class="badge bg-warning text-dark">Pending</span>'}
                  </td>
                  <td>
                    <div class="btn-group btn-group-sm">
                      ${app.status === 'APPLIED' ? `
                        <button class="btn btn-success" onclick="Owner.acceptApplicant(${app.id})"><i class="bi bi-check2"></i> Accept</button>
                        <button class="btn btn-outline-danger" onclick="Owner.rejectApplicant(${app.id})"><i class="bi bi-x"></i> Reject</button>
                      ` : ''}
                      
                      ${(app.status === 'ACCEPTED' || app.status === 'COMPLETED') && app.paymentStatus !== 'PAID' && app.paymentStatus !== 'CONFIRMED' ? `
                        <button class="btn btn-outline-success" onclick="Owner.markPaymentPaid(${app.id}, ${app.paymentAmount})">
                          <i class="bi bi-cash-stack"></i> Mark Paid
                        </button>
                      ` : ''}
                    </div>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (err) {
      container.innerHTML = `<div class="text-center text-danger p-3">Failed to load applicants: ${err.message}</div>`;
    }
  },

  async acceptApplicant(appId) {
    try {
      await API.owner.acceptApplicant(appId);
      App.showToast('Student accepted! Worker limit and status updated.', 'success');
      if (this.currentActiveJobId) {
        await this.openApplicantsModal(this.currentActiveJobId, document.getElementById('applicantsJobTitle').innerText);
      }
      await this.loadMyJobs();
    } catch (err) {
      App.showToast(err.message || 'Failed to accept applicant', 'danger');
    }
  },

  async rejectApplicant(appId) {
    if (!confirm('Reject this applicant?')) return;
    try {
      await API.owner.rejectApplicant(appId);
      App.showToast('Applicant rejected', 'info');
      if (this.currentActiveJobId) {
        await this.openApplicantsModal(this.currentActiveJobId, document.getElementById('applicantsJobTitle').innerText);
      }
      await this.loadMyJobs();
    } catch (err) {
      App.showToast(err.message || 'Failed to reject applicant', 'danger');
    }
  },

  async updateAttendance(appId, attendanceStatus) {
    try {
      await API.owner.markAttendance(appId, attendanceStatus, 'COMPLETED');
      App.showToast('Attendance updated to ' + attendanceStatus, 'success');
    } catch (err) {
      App.showToast(err.message || 'Failed to update attendance', 'danger');
    }
  },

  async completeJob(jobId) {
    if (!confirm('Are you sure you want to mark this catering job as COMPLETED?')) return;
    try {
      await API.owner.completeJob(jobId);
      App.showToast('Job marked as completed successfully!', 'success');
      await this.loadMyJobs();
    } catch (err) {
      App.showToast(err.message || 'Failed to complete job', 'danger');
    }
  },

  async cancelJob(jobId) {
    if (!confirm('Are you sure you want to cancel this job? Applied students will be notified.')) return;
    try {
      await API.owner.cancelJob(jobId);
      App.showToast('Job cancelled', 'info');
      await this.loadMyJobs();
    } catch (err) {
      App.showToast(err.message || 'Failed to cancel job', 'danger');
    }
  },

  async markPaymentPaid(appId, amount) {
    const notes = prompt(`Mark ₹${amount} as PAID to student. Enter payment notes (optional, e.g. "Cash On-Spot"):`, 'Paid cash on-spot');
    if (notes === null) return;

    try {
      await API.owner.markPaymentPaid(appId, notes);
      App.showToast('Payment marked as PAID. Student notified to confirm receipt.', 'success');
      if (this.currentActiveJobId) {
        await this.openApplicantsModal(this.currentActiveJobId, document.getElementById('applicantsJobTitle').innerText);
      }
      await this.loadPayments();
    } catch (err) {
      App.showToast(err.message || 'Failed to mark payment', 'danger');
    }
  },

  async loadPayments() {
    const container = document.getElementById('owPaymentsList');
    if (!container) return;

    try {
      container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading payments...</td></tr>';
      const payments = await API.owner.getPayments();

      let totalPaid = 0;
      if (payments && payments.length > 0) {
        payments.forEach(p => {
          if (p.paymentStatus === 'PAID' || p.paymentStatus === 'CONFIRMED') {
            totalPaid += (p.amount || 0);
          }
        });
      }
      document.getElementById('owStatTotalPayments').innerText = '₹' + totalPaid.toLocaleString();

      if (!payments || payments.length === 0) {
        container.innerHTML = '<tr><td colspan="6" class="text-center p-4 text-muted">No payment records found yet.</td></tr>';
        return;
      }

      container.innerHTML = payments.map(pay => `
        <tr>
          <td class="fw-bold">${App.escapeHtml(pay.jobTitle)}</td>
          <td>${App.escapeHtml(pay.studentName)}</td>
          <td class="fw-bold text-success">₹${pay.amount}</td>
          <td><span class="badge bg-light text-dark border">${pay.paymentTypeDisplayName || 'On-Spot'}</span></td>
          <td>
            ${pay.paymentStatus === 'CONFIRMED' ? '<span class="badge bg-success"><i class="bi bi-check-all me-1"></i>Confirmed by Student</span>' :
              pay.paymentStatus === 'PAID' ? '<span class="badge bg-info text-dark">Marked Paid</span>' :
              pay.paymentStatus === 'DISPUTED' ? '<span class="badge bg-danger">Disputed</span>' :
              '<span class="badge bg-warning text-dark">Pending</span>'}
          </td>
          <td>${App.formatDate(pay.markedPaidAt || pay.createdAt)}</td>
        </tr>
      `).join('');
    } catch (err) {
      container.innerHTML = `<tr><td colspan="6" class="text-center p-3 text-danger">Failed to load payments</td></tr>`;
    }
  },

  async loadComplaints() {
    const container = document.getElementById('owComplaintsList');
    if (!container) return;

    try {
      container.innerHTML = '<div class="text-center p-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Loading complaints...</div>';
      const complaints = await API.owner.getComplaints();

      if (!complaints || complaints.length === 0) {
        container.innerHTML = '<div class="text-center p-4 text-muted"><i class="bi bi-chat-square-check fs-2 d-block mb-2"></i>No student complaints have been received.</div>';
        return;
      }

      container.innerHTML = complaints.map(report => `
        <article class="card border mb-3 shadow-sm">
          <div class="card-body">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
              <div>
                <span class="badge bg-danger-subtle text-danger mb-2">${App.escapeHtml(report.reportType || 'Complaint')}</span>
                <h6 class="fw-bold mb-1">${App.escapeHtml(report.jobTitle || 'Job not available')}</h6>
                <small class="text-muted">Reported ${App.formatDate(report.createdAt)}${report.workArea ? ` &middot; ${App.escapeHtml(report.workArea)}` : ''}</small>
              </div>
              <span class="badge ${report.status === 'RESOLVED' ? 'bg-success' : 'bg-warning text-dark'}">${App.escapeHtml(report.status || 'PENDING')}</span>
            </div>

            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <div class="p-3 bg-light rounded h-100">
                  <div class="small text-muted mb-1">Student details</div>
                  <div class="fw-bold">${App.escapeHtml(report.studentName || 'Student')}</div>
                  <div class="small mt-1"><i class="bi bi-telephone me-1"></i><a href="tel:${App.escapeHtml(report.studentPhone || '')}">${App.escapeHtml(report.studentPhone || 'Phone unavailable')}</a></div>
                  <div class="small"><i class="bi bi-envelope me-1"></i><a href="mailto:${App.escapeHtml(report.studentEmail || '')}">${App.escapeHtml(report.studentEmail || 'Email unavailable')}</a></div>
                  <div class="small text-muted mt-1">Skills: ${App.escapeHtml(report.studentSkills || 'Not provided')}</div>
                  <div class="small text-muted">Area: ${App.escapeHtml(report.studentArea || 'Not provided')}</div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="p-3 bg-light rounded h-100">
                  <div class="small text-muted mb-1">Payment and application</div>
                  <div class="small">Expected: <strong>₹${App.escapeHtml(report.expectedAmount ?? 'Not provided')}</strong></div>
                  <div class="small">Received: <strong>₹${App.escapeHtml(report.receivedAmount ?? '0')}</strong></div>
                  <div class="small">Application: <strong>${App.escapeHtml(report.applicationStatus || 'Not available')}</strong></div>
                  <div class="small">Payment status: <strong>${App.escapeHtml(report.applicationPaymentStatus || 'Not available')}</strong></div>
                </div>
              </div>
            </div>

            <div class="mb-2"><strong>Student&apos;s message:</strong> <span class="text-muted">${App.escapeHtml(report.description || 'No description provided')}</span></div>
            <div class="small text-muted mb-3"><strong>Evidence / additional notes:</strong> ${App.escapeHtml(report.evidenceNotes || 'None provided')}</div>
            <div class="alert alert-info py-2 mb-0 small"><i class="bi bi-chat-dots me-1"></i>Contact the student by phone or email above to discuss the issue, then cooperate with the platform admin review.</div>
          </div>
        </article>
      `).join('');
    } catch (err) {
      container.innerHTML = `<div class="text-center text-danger p-3">Failed to load complaints: ${App.escapeHtml(err.message)}</div>`;
    }
  }
};
