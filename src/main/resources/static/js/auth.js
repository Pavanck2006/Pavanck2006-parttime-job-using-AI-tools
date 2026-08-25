/**
 * PARTTIME JOB PLATFORM - AUTHENTICATION & SESSION MANAGEMENT
 */

const Auth = {
  init() {
    this.bindEvents();
    this.updateNavigation();
  },

  bindEvents() {
    // Login form submission
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
      loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleLogin();
      });
    }

    // Register form submission
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
      registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleRegister();
      });
    }

    // Role switcher in registration
    const roleRadios = document.querySelectorAll('input[name="registerRole"]');
    roleRadios.forEach(radio => {
      radio.addEventListener('change', (e) => {
        this.toggleRegistrationFields(e.target.value);
      });
    });

    // Quick Fill demo buttons
    document.querySelectorAll('.quick-fill-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const email = e.currentTarget.getAttribute('data-email');
        const pass = e.currentTarget.getAttribute('data-pass');
        document.getElementById('loginEmail').value = email;
        document.getElementById('loginPassword').value = pass;
      });
    });
  },

  toggleRegistrationFields(role) {
    const studentFields = document.getElementById('studentRegisterFields');
    const ownerFields = document.getElementById('ownerRegisterFields');

    if (role === 'ROLE_STUDENT') {
      studentFields?.classList.remove('d-none');
      ownerFields?.classList.add('d-none');
    } else {
      studentFields?.classList.add('d-none');
      ownerFields?.classList.remove('d-none');
    }
  },

  async handleLogin() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    const btn = document.getElementById('loginSubmitBtn');

    try {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Logging in...';

      const authData = await API.auth.login(email, password);
      API.setAuthData(authData);

      App.showToast('Welcome back, ' + authData.fullName + '!', 'success');
      
      // Close modal
      const modalEl = document.getElementById('loginModal');
      const modal = bootstrap.Modal.getInstance(modalEl);
      if (modal) modal.hide();

      this.updateNavigation();
      App.routeToRoleDashboard(authData.role);
    } catch (err) {
      App.showToast(err.message || 'Login failed', 'danger');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i>Sign In';
    }
  },

  async handleRegister() {
    const role = document.querySelector('input[name="registerRole"]:checked').value;
    const fullName = document.getElementById('regFullName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const phone = document.getElementById('regPhone').value.trim();
    const btn = document.getElementById('registerSubmitBtn');
    const payload = {
      fullName,
      email,
      password,
      phone,
      role
    };

    if (!document.getElementById('regVerificationId').value) {
      try {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Sending Code...';
        const otp = await API.auth.requestOtp({email});
        document.getElementById('regVerificationId').value = otp.verificationId;
        document.getElementById('regOtpGroup').classList.remove('d-none');
        btn.disabled = true;
        btn.innerHTML = '<i class="bi bi-person-check me-2"></i>Create Account';
        App.showToast('Verification code sent by email. Check your inbox.', 'info');
      } catch (err) { btn.disabled = false; App.showToast(err.message || 'Could not send verification code', 'danger'); }
      return;
    }
    if (document.getElementById('regEmailVerified').value !== 'true') {
      App.showToast('Verify your email before creating the account.', 'warning');
      return;
    }
    payload.verificationId = document.getElementById('regVerificationId').value;

    if (role === 'ROLE_STUDENT') {
      payload.preferredArea = document.getElementById('regPrefArea').value.trim();
      payload.skills = Array.from(document.querySelectorAll('.reg-skill:checked'))
        .map(skill => skill.value)
        .join(', ');
    } else {
      payload.cateringName = document.getElementById('regCateringName').value.trim();
      payload.businessAddress = document.getElementById('regBusinessAddress').value.trim();
      payload.businessPhone = document.getElementById('regBusinessPhone').value.trim() || phone;
    }

    try {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating Account...';

      const authData = await API.auth.register(payload);
      API.setAuthData(authData);

      App.showToast('Account created successfully! Welcome to PartTime Job.', 'success');

      // Close modal
      const modalEl = document.getElementById('registerModal');
      const modal = bootstrap.Modal.getInstance(modalEl);
      if (modal) modal.hide();

      this.updateNavigation();
      App.routeToRoleDashboard(authData.role);
    } catch (err) {
      App.showToast(err.message || 'Registration failed', 'danger');
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-person-check me-2"></i>Create Account';
    }
  },

  async handleVerifyEmail() {
    const verificationId = document.getElementById('regVerificationId').value;
    const otp = document.getElementById('regOtp').value.trim();
    const btn = document.getElementById('verifyEmailBtn');
    if (!verificationId) return App.showToast('Request a verification code first.', 'warning');
    if (!/^\d{6}$/.test(otp)) return App.showToast('Enter the complete 6-digit OTP.', 'warning');
    try {
      btn.disabled = true;
      const result = await API.auth.verifyOtp({verificationId, otp});
      document.getElementById('regEmailVerified').value = 'true';
      document.getElementById('regOtp').disabled = true;
      document.getElementById('regOtpGroup').classList.add('d-none');
      document.getElementById('registerSubmitBtn').disabled = false;
      btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Email Verified';
      App.showToast(result?.message || 'Email verified successfully.', 'success');
    } catch (err) {
      btn.disabled = false;
      App.showToast(err.message || 'Email verification failed', 'danger');
    }
  },

  resendOtp() {
    document.getElementById('regVerificationId').value = '';
    document.getElementById('regEmailVerified').value = '';
    document.getElementById('regOtp').value = '';
    document.getElementById('regOtp').disabled = false;
    document.getElementById('regOtpGroup').classList.add('d-none');
    document.getElementById('verifyEmailBtn').disabled = false;
    document.getElementById('verifyEmailBtn').innerHTML = '<i class="bi bi-shield-check me-1"></i>Verify Email';
    document.getElementById('registerForm').requestSubmit();
  },


  logout() {
    API.clearAuthData();
    App.showToast('You have been logged out.', 'info');
    this.updateNavigation();
    App.navigate('home');
  },

  updateNavigation() {
    const user = API.getCurrentUser();
    const guestNav = document.getElementById('guestNav');
    const authNav = document.getElementById('authNav');
    const userDisplayName = document.getElementById('userDisplayName');
    const userRoleBadge = document.getElementById('userRoleBadge');
    const dashboardNavBtn = document.getElementById('dashboardNavBtn');
    const studentApplicationsSetting = document.getElementById('studentApplicationsSetting');

    if (user && API.isAuthenticated()) {
      guestNav?.classList.add('d-none');
      authNav?.classList.remove('d-none');
      dashboardNavBtn?.classList.remove('d-none');
      studentApplicationsSetting?.classList.toggle('d-none', user.role !== 'ROLE_STUDENT');

      if (userDisplayName) userDisplayName.innerText = user.fullName;

      if (userRoleBadge) {
        if (user.role === 'ROLE_ADMIN') {
          userRoleBadge.className = 'badge bg-danger ms-1';
          userRoleBadge.innerText = 'Admin';
        } else if (user.role === 'ROLE_OWNER') {
          userRoleBadge.className = 'badge bg-primary ms-1';
          userRoleBadge.innerText = user.cateringName || 'Owner';
        } else {
          userRoleBadge.className = 'badge bg-success ms-1';
          userRoleBadge.innerText = 'Student';
        }
      }

      // Start notification polling
      Notifications.startPolling();
    } else {
      guestNav?.classList.remove('d-none');
      authNav?.classList.add('d-none');
      dashboardNavBtn?.classList.add('d-none');
      studentApplicationsSetting?.classList.add('d-none');
      Notifications.stopPolling();
    }
  }
};
