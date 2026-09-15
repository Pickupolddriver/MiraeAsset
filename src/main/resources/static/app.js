const state = {
    page: 0,
    size: 6,
    query: '',
    category: '',
    books: [],
    totalPages: 1,
    loans: [],
    activeBook: null
};

const $ = (selector) => document.querySelector(selector);

function userId() {
    return $('#user-id').value.trim();
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        ...options,
        headers: {
            ...(options.body ? {'Content-Type': 'application/json'} : {}),
            ...(options.method && options.method !== 'GET' ? {'X-User-Id': userId()} : {}),
            ...(options.headers || {})
        }
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = new Error(payload.detail || payload.message || 'Something went wrong.');
        error.code = payload.code;
        error.status = response.status;
        throw error;
    }
    return payload;
}

function showToast(message, isError = false) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.classList.add('visible');
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => toast.classList.remove('visible'), 3300);
}

function friendlyMessage(error) {
    const messages = {
        ACTIVE_LOAN_ALREADY_EXISTS: 'This book is already on your shelf.',
        BOOK_UNAVAILABLE: 'All digital copies of this book are currently borrowed.',
        INVALID_USER: 'Please enter a valid reader identity.'
    };
    return messages[error.code] || error.message;
}
function renderBooks() {
    const grid = $('#book-grid');
    $('#result-count').textContent = `${state.totalElements || 0} ${state.totalElements === 1 ? 'title' : 'titles'}`;
    $('#page-label').textContent = `Page ${state.page + 1}`;
    $('#pagination-label').textContent = `${state.page + 1} / ${state.totalPages}`;
    $('#previous-page').disabled = state.page === 0;
    $('#next-page').disabled = state.page >= state.totalPages - 1;
    $('#pagination').hidden = state.totalPages <= 1;

    if (!state.books.length) {
        grid.innerHTML = '<div class="error-state">No titles matched your search.</div>';
        return;
    }
    const activeBookIds = new Set(state.loans.map((loan) => loan.bookId));
    grid.innerHTML = state.books.map((book, index) => {
        const available = book.availableLicenses > 0;
        const alreadyBorrowed = activeBookIds.has(book.id);
        const actionLabel = alreadyBorrowed ? 'On your shelf' : available ? 'View details' : 'Unavailable';
        return `<article class="book-card">
            <div class="book-cover"><span class="book-number">NO. ${String(index + 1 + state.page * state.size).padStart(2, '0')}</span></div>
            <span class="book-category">${escapeHtml(book.category)}</span>
            <h3>${escapeHtml(book.title)}</h3>
            <p class="book-author">${escapeHtml(book.author)}</p>
            <div class="book-bottom"><span class="availability"><strong>${book.availableLicenses}</strong> / ${book.totalLicenses} available</span>
                <button class="text-button" type="button" data-book-id="${book.id}" ${alreadyBorrowed || !available ? 'disabled' : ''}>${actionLabel} ${alreadyBorrowed ? '✓' : '↗'}</button>
            </div>
        </article>`;
    }).join('');
    grid.querySelectorAll('[data-book-id]').forEach((button) => button.addEventListener('click', () => openBook(Number(button.dataset.bookId))));
}

function renderLoans() {
    $('#loan-count').textContent = state.loans.length;
    const list = $('#loan-list');
    if (!state.loans.length) {
        list.innerHTML = '<div class="empty-state"><span class="empty-symbol">＋</span><strong>Your shelf is open.</strong><span>Borrow a book to start reading.</span></div>';
        return;
    }
    list.innerHTML = state.loans.map((loan) => `<div class="loan-item">
        <div class="loan-thumb">READ</div><div><strong title="${escapeHtml(loan.bookTitle)}">${escapeHtml(loan.bookTitle)}</strong>
        <small>Due ${formatDate(loan.dueAt)}</small><button class="return-button" type="button" data-loan-id="${loan.loanId}">Return book ↗</button></div>
    </div>`).join('');
    list.querySelectorAll('[data-loan-id]').forEach((button) => button.addEventListener('click', () => returnLoan(button.dataset.loanId, button)));
}

async function loadBooks() {
    const params = new URLSearchParams({page: state.page, size: state.size});
    if (state.query) params.set('q', state.query);
    if (state.category) params.set('category', state.category);
    $('#book-grid').innerHTML = '<div class="loading-state"><span class="spinner"></span>Loading the collection...</div>';
    try {
        const payload = await api(`/api/books?${params}`);
        state.books = payload.content;
        state.totalElements = payload.totalElements;
        state.totalPages = Math.max(payload.totalPages, 1);
        renderBooks();
    } catch (error) {
        $('#book-grid').innerHTML = `<div class="error-state"><span>${escapeHtml(error.message)}</span><button id="retry-books" class="text-button" type="button">Try again ↗</button></div>`;
        $('#retry-books').addEventListener('click', loadBooks);
        showToast(friendlyMessage(error), true);
    }
}

async function loadLoans() {
    if (!userId()) {
        state.loans = [];
        renderLoans();
        return;
    }
    $('#loan-list').innerHTML = '<div class="loading-state"><span class="spinner"></span>Updating your shelf...</div>';
    try {
        state.loans = await api('/api/loans/current', {headers: {'X-User-Id': userId()}});
        renderLoans();
    } catch (error) {
        $('#loan-list').innerHTML = `<div class="error-state"><span>${escapeHtml(error.message)}</span><button id="retry-loans" class="text-button" type="button">Try again ↗</button></div>`;
        $('#retry-loans').addEventListener('click', loadLoans);
        showToast(friendlyMessage(error), true);
    }
}

async function refreshLibrary() {
    await loadLoans();
    await loadBooks();
}
async function openBook(bookId) {
    try {
        state.activeBook = await api(`/api/books/${bookId}`);
        const book = state.activeBook;
        $('#modal-title').textContent = book.title;
        $('#modal-author').textContent = book.author;
        $('#modal-category').textContent = book.category;
        $('#modal-isbn').textContent = `ISBN ${book.isbn}`;
        $('#modal-description').textContent = book.description;
        $('#modal-availability-value').textContent = `${book.availableLicenses} of ${book.totalLicenses} available`;
        $('#borrow-button').disabled = book.availableLicenses < 1;
        $('#borrow-button').textContent = book.availableLicenses < 1 ? 'Currently unavailable' : 'Borrow book ↗';
        $('#book-modal').hidden = false;
        document.body.style.overflow = 'hidden';
    } catch (error) {
        showToast(friendlyMessage(error), true);
    }
}

async function borrowBook() {
    if (!state.activeBook) return;
    if (!userId()) { showToast('Enter a reader identity first.', true); return; }
    const button = $('#borrow-button');
    const borrowedTitle = state.activeBook.title;
    button.disabled = true;
    try {
        await api('/api/loans', {method: 'POST', body: JSON.stringify({bookId: state.activeBook.id})});
        closeModal();
        showToast(`${borrowedTitle} added to your shelf.`);
        await refreshLibrary();
    } catch (error) {
        const message = friendlyMessage(error);
        if (error.status === 409) {
            closeModal();
            await refreshLibrary();
        }
        showToast(message, true);
        button.disabled = false;
    }
}

async function returnLoan(loanId, button) {
    if (button) button.disabled = true;
    try {
        await api(`/api/loans/${loanId}/return`, {method: 'PUT'});
        showToast('Book returned to the collection.');
        await refreshLibrary();
    } catch (error) {
        showToast(friendlyMessage(error), true);
        if (button) button.disabled = false;
    }
}

function closeModal() { $('#book-modal').hidden = true; document.body.style.overflow = ''; state.activeBook = null; }
function formatDate(value) { return new Intl.DateTimeFormat('en', {month: 'short', day: 'numeric'}).format(new Date(value)); }
function escapeHtml(value) { return String(value ?? '').replace(/[&<>'"]/g, (char) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[char])); }

function search() { state.query = $('#search-input').value.trim(); state.page = 0; loadBooks(); }

$('#search-button').addEventListener('click', search);
$('#search-input').addEventListener('keydown', (event) => { if (event.key === 'Enter') search(); });
$('#previous-page').addEventListener('click', () => { if (state.page > 0) { state.page--; loadBooks(); } });
$('#next-page').addEventListener('click', () => { if (state.page < state.totalPages - 1) { state.page++; loadBooks(); } });
$('#borrow-button').addEventListener('click', borrowBook);
document.querySelectorAll('[data-close-modal]').forEach((element) => element.addEventListener('click', closeModal));
document.addEventListener('keydown', (event) => { if (event.key === 'Escape' && !$('#book-modal').hidden) closeModal(); });
$('#user-id').addEventListener('change', () => { localStorage.setItem('elibrary-user-id', userId()); refreshLibrary(); });
document.querySelectorAll('[data-category]').forEach((button) => button.addEventListener('click', () => {
    state.category = button.dataset.category;
    state.page = 0;
    document.querySelectorAll('[data-category]').forEach((item) => item.classList.toggle('selected', item === button));
    loadBooks();
}));

const savedUserId = localStorage.getItem('elibrary-user-id');
if (savedUserId) $('#user-id').value = savedUserId;
refreshLibrary();
