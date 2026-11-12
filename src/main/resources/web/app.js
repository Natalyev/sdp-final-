const $ = (selector) => document.querySelector(selector);
const state = { data: null, activeId: 'CONCERT-1', lastActivity: 0, loaded: false };

const eventNames = {
  'CONCERT-1': { title: 'Concert', subtitle: 'Live music event', category: 'Concert', icon: '🎵' },
  'FLIGHT-1': { title: 'Flight', subtitle: 'Flight ticket', category: 'Flight', icon: '✈️' }
};

function safe(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

function details(event) {
  return eventNames[event.id] ?? { title: event.id, subtitle: 'Ticket event', category: 'Event', icon: '🎟️' };
}

function currentEvent() {
  return state.data?.events.find((event) => event.id === state.activeId) ?? state.data?.events[0];
}

function nextSeat(event) {
  if (!event || !event.availableSeats.length) return null;
  return event.policy === 'back' ? Math.max(...event.availableSeats) : Math.min(...event.availableSeats);
}

function money(price) {
  return `$${Number(price).toFixed(2)}`;
}

function notify(message, error = false) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.toggle('error', error);
  toast.classList.add('show');
  clearTimeout(notify.timer);
  notify.timer = setTimeout(() => toast.classList.remove('show'), 3600);
}

function renderEvents() {
  $('#event-grid').innerHTML = state.data.events.map((event) => {
    const info = details(event);
    const available = event.availableSeats.length;
    return `<article class="event-card ${event.id === state.activeId ? 'selected' : ''}">
      <div class="event-icon" aria-hidden="true">${info.icon}</div>
      <span class="event-category">${safe(info.category)}</span>
      <h3>${safe(info.title)}</h3><p>${safe(info.subtitle)}</p>
      <div class="event-info"><span>Ticket price</span><strong>${money(event.price)}</strong></div>
      <div class="event-bottom"><span>${available} of ${event.allSeats.length} seats available</span><button type="button" data-event="${safe(event.id)}">Select</button></div>
    </article>`;
  }).join('');
}

function renderSeats() {
  const event = currentEvent();
  if (!event) return;
  const info = details(event);
  const available = new Set(event.availableSeats);
  const next = nextSeat(event);
  $('#seat-event-title').textContent = info.title;
  $('#seat-availability').textContent = `${available.size} / ${event.allSeats.length} OPEN`;
  $('#stage-label').textContent = event.kind === 'flight' ? 'FRONT OF CABIN' : 'THE STAGE';
  $('#seat-grid').innerHTML = event.allSeats.map((seat) => `<span class="seat ${available.has(seat) ? (seat === next ? 'next' : '') : 'taken'}" title="Seat ${seat}: ${available.has(seat) ? 'available' : 'taken'}">${seat}</span>`).join('');
  $('#selected-experience').textContent = info.title;
  $('#next-seat').textContent = next === null ? 'Sold out' : `Seat ${next}`;
  $('#selected-price').textContent = money(event.price);
  $('#book-button').disabled = next === null;
  $('#price-input').placeholder = Number(event.price).toFixed(2);
  document.querySelectorAll('#strategy-control button').forEach((button) => {
    button.classList.toggle('active', button.dataset.policy === event.policy);
    button.setAttribute('aria-pressed', String(button.dataset.policy === event.policy));
  });
}

function renderBookings() {
  const reservations = state.data.reservations;
  $('#booking-count').textContent = `${reservations.length} booking${reservations.length === 1 ? '' : 's'}`;
  $('#booking-list').innerHTML = reservations.length ? reservations.map((item) => {
    const status = item.status.toLowerCase();
    return `<div class="booking-item"><div><span class="status ${status}">${safe(item.status)}</span><strong>${safe(details({ id: item.eventId }).title)} · Seat ${item.seat}</strong><small>${safe(item.id)} · ${money(item.price)} · ${safe(item.category)}</small></div>
      <div class="booking-actions">${item.status === 'PENDING' ? `<button type="button" data-action="confirm" data-id="${safe(item.id)}">Confirm</button>` : ''}${item.status !== 'CANCELLED' ? `<button type="button" class="cancel" data-action="cancel" data-id="${safe(item.id)}">Cancel</button>` : ''}</div></div>`;
  }).join('') : '<div class="empty-state">No bookings for this customer yet.<br>Pick an experience and reserve a seat above.</div>';
}

function renderActivity() {
  const items = state.data.activity.slice(0, 5);
  $('#activity-list').innerHTML = items.length ? items.map((item) => {
    const price = item.type === 'PRICE_CHANGED';
    const name = details({ id: item.eventId }).title;
    return `<div class="activity-item ${price ? 'price' : ''}"><span class="activity-icon">${price ? '$' : '↗'}</span><div><strong>${price ? 'Price updated' : 'Availability changed'}</strong><small>${safe(name)} · ${price ? `${money(item.price)} per seat` : `${item.seatsRemaining} seats remaining`}</small></div></div>`;
  }).join('') : '<div class="empty-state">Your live updates will appear here after the first action.</div>';
}

function render() {
  if (!state.data.events.some((event) => event.id === state.activeId)) state.activeId = state.data.events[0]?.id;
  renderEvents();
  renderSeats();
  renderBookings();
  renderActivity();
}

async function refresh() {
  const customer = $('#customer-id').value.trim();
  const response = await fetch(`/api/state?customer=${encodeURIComponent(customer)}`, { cache: 'no-store' });
  if (!response.ok) throw new Error('Could not load reservation data');
  state.data = await response.json();
  const newest = state.data.activity[0]?.id ?? 0;
  if (state.loaded && newest > state.lastActivity) {
    const item = state.data.activity[0];
    notify(item.type === 'PRICE_CHANGED' ? 'A ticket price just changed.' : 'Seat availability just changed.');
  }
  state.lastActivity = newest;
  state.loaded = true;
  render();
}

async function action(values, success) {
  try {
    const response = await fetch('/api/action', { method: 'POST', body: new URLSearchParams(values) });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || 'Something went wrong');
    await refresh();
    notify(success(result));
  } catch (error) {
    notify(error.message || 'Action failed', true);
  }
}

$('#event-grid').addEventListener('click', (event) => {
  const button = event.target.closest('[data-event]');
  if (!button) return;
  state.activeId = button.dataset.event;
  render();
  $('#reserve').scrollIntoView({ behavior: 'smooth', block: 'start' });
});

$('#book-button').addEventListener('click', () => {
  const customer = $('#customer-id').value.trim();
  if (!customer) return notify('Enter a customer ID first.', true);
  action({ action: 'book', event: state.activeId, customer }, (result) => `Seat ${result.reservation.seat} reserved! Confirm it below.`);
});

$('#booking-list').addEventListener('click', (event) => {
  const button = event.target.closest('[data-action]');
  if (!button) return;
  action({ action: button.dataset.action, id: button.dataset.id }, () => button.dataset.action === 'confirm' ? 'Booking confirmed.' : 'Booking cancelled. The seat is free again.');
});

$('#strategy-control').addEventListener('click', (event) => {
  const button = event.target.closest('[data-policy]');
  if (!button || button.dataset.policy === currentEvent()?.policy) return;
  action({ action: 'strategy', event: state.activeId, policy: button.dataset.policy }, () => 'Seating policy updated. Watch the next seat move.');
});

$('#price-button').addEventListener('click', () => {
  const price = $('#price-input').value.trim();
  if (!price || Number(price) < 0) return notify('Enter a valid non-negative price.', true);
  action({ action: 'price', event: state.activeId, price }, () => 'Ticket price updated.');
  $('#price-input').value = '';
});

$('#customer-id').addEventListener('change', () => {
  localStorage.setItem('seatly-customer', $('#customer-id').value.trim());
  refresh().catch(() => notify('Could not refresh bookings.', true));
});

$('#customer-id').value = localStorage.getItem('seatly-customer') || 'GUEST-01';
refresh().catch(() => notify('Cannot connect to the local server. Start tickets.Main --web.', true));
setInterval(() => refresh().catch(() => {}), 4000);
