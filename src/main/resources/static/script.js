// API Configuration
const API_BASE_URL = '';  // Empty string means use same origin

// Shorten URL Function
async function shortenUrl() {
    const longUrl = document.getElementById('longUrl').value.trim();

    if (!longUrl) {
        showToast('Please enter a URL', 'error');
        return;
    }

    // Validate URL format
    const urlPattern = /^(https?:\/\/)/i;
    if (!urlPattern.test(longUrl)) {
        showToast('Please enter a valid URL starting with http:// or https://', 'error');
        return;
    }

    const shortenBtn = document.getElementById('shortenBtn');
    shortenBtn.disabled = true;
    shortenBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Shortening...';

    try {
        // Use relative URL since we're on the same server
        const response = await fetch('/shorten', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ longUrl: longUrl })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to shorten URL');
        }

        const data = await response.json();

        // Display the result
        document.getElementById('shortUrl').textContent = data.shortUrl;
        document.getElementById('clickCount').textContent = data.clickCount || 0;
        document.getElementById('createdAt').textContent = formatDate(data.createdAt);
        document.getElementById('resultSection').style.display = 'block';

        showToast('URL shortened successfully!', 'success');

        // Clear input
        document.getElementById('longUrl').value = '';

        // Scroll to result
        document.getElementById('resultSection').scrollIntoView({ behavior: 'smooth' });

        // Add to dashboard if you have one
        addToDashboard(data);

    } catch (error) {
        console.error('Error:', error);
        showToast(error.message || 'Failed to shorten URL. Please try again.', 'error');
    } finally {
        shortenBtn.disabled = false;
        shortenBtn.innerHTML = '<i class="fas fa-cut"></i> Shorten URL';
    }
}

// Helper function to format date
function formatDate(dateString) {
    if (!dateString) return 'Just now';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    } catch (e) {
        return 'Just now';
    }
}

// Add to dashboard
function addToDashboard(data) {
    let links = JSON.parse(localStorage.getItem('snipurl_links') || '[]');
    links.unshift(data);
    links = links.slice(0, 10); // Keep only last 10
    localStorage.setItem('snipurl_links', JSON.stringify(links));
    renderDashboard();
}

// Render dashboard
function renderDashboard() {
    const links = JSON.parse(localStorage.getItem('snipurl_links') || '[]');
    const tableBody = document.getElementById('linksTableBody');

    if (!tableBody) return;

    if (links.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="empty-state">
                    <i class="fas fa-link"></i>
                    <p>No links yet. Create your first shortened URL!</p>
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = links.map(link => `
        <tr>
            <td>
                <a href="${link.shortUrl}" target="_blank" class="short-link">
                    ${link.shortUrl}
                </a>
            </td>
            <td title="${link.longUrl}">
                ${truncateUrl(link.longUrl, 50)}
            </td>
            <td>${link.clickCount || 0}</td>
            <td>${formatDate(link.createdAt)}</td>
            <td class="actions">
                <button class="action-btn" onclick="copyLink('${link.shortUrl}')" title="Copy">
                    <i class="fas fa-copy"></i>
                </button>
                <button class="action-btn" onclick="viewAnalytics('${link.shortCode}')" title="Analytics">
                    <i class="fas fa-chart-line"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function truncateUrl(url, length) {
    if (url.length <= length) return url;
    return url.substring(0, length) + '...';
}

function copyLink(url) {
    navigator.clipboard.writeText(url);
    showToast('Link copied!', 'success');
}

function viewAnalytics(shortCode) {
    showToast('Analytics feature coming soon!', 'info');
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
        <span>${message}</span>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    const shortenBtn = document.getElementById('shortenBtn');
    const longUrlInput = document.getElementById('longUrl');
    const copyBtn = document.getElementById('copyBtn');

    if (shortenBtn) {
        shortenBtn.addEventListener('click', shortenUrl);
    }

    if (copyBtn) {
        copyBtn.addEventListener('click', () => {
            const shortUrl = document.getElementById('shortUrl')?.textContent;
            if (shortUrl) {
                navigator.clipboard.writeText(shortUrl);
                showToast('Copied to clipboard!', 'success');
            }
        });
    }

    if (longUrlInput) {
        longUrlInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                shortenUrl();
            }
        });
    }

    renderDashboard();
});