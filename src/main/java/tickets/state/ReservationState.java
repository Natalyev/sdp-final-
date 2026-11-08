package tickets.state;

interface ReservationState {
    ReservationStatus status();
    ReservationState confirm();
    ReservationState cancel();
}

final class PendingState implements ReservationState {
    public ReservationStatus status() { return ReservationStatus.PENDING; }
    public ReservationState confirm() { return new ConfirmedState(); }
    public ReservationState cancel() { return new CancelledState(); }
}

final class ConfirmedState implements ReservationState {
    public ReservationStatus status() { return ReservationStatus.CONFIRMED; }
    public ReservationState confirm() { return this; }
    public ReservationState cancel() { return new CancelledState(); }
}

final class CancelledState implements ReservationState {
    public ReservationStatus status() { return ReservationStatus.CANCELLED; }
    public ReservationState confirm() { throw new IllegalStateException("Cancelled reservation cannot be confirmed"); }
    public ReservationState cancel() { throw new IllegalStateException("Reservation is already cancelled"); }
}
