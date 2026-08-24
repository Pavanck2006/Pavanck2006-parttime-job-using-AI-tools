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

    if (role === 'ROLE_STUDENT') {
      payload.collegeName = document.getElementById('regCollege').value.trim();
      payload.preferredArea = document.getElementById('regPrefArea').value.trim();
      payload.skills = document.getElementById('regSkills').value.trim();
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

    if (user && API.isAuthenticated()) {
      guestNav?.classList.add('d-none');
      authNav?.classList.remove('d-none');
      dashboardNavBtn?.classList.remove('d-none');

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
      Notifications.stopPolling();
    }
  }
};
