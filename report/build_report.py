"""Build the final PDF report from the verified project summary and run logs."""

from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate, Flowable, Frame, KeepTogether, PageBreak, PageTemplate,
    Paragraph, Preformatted, Spacer, Table, TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "FinalReport.pdf"
OUTPUT.parent.mkdir(parents=True, exist_ok=True)

NAVY = colors.HexColor("#17243B")
TEAL = colors.HexColor("#0A8B86")
INK = colors.HexColor("#243149")
MUTED = colors.HexColor("#65758A")
PALE = colors.HexColor("#EAF3F3")
RULE = colors.HexColor("#DCE4EA")

styles = getSampleStyleSheet()
title = ParagraphStyle("CoverTitle", parent=styles["Title"], fontName="Helvetica-Bold",
                       fontSize=29, leading=34, textColor=NAVY, alignment=TA_LEFT)
subtitle = ParagraphStyle("CoverSub", parent=styles["Normal"], fontSize=13, leading=19,
                          textColor=TEAL)
h1 = ParagraphStyle("H1Custom", parent=styles["Heading1"], fontSize=15, leading=19,
                    textColor=NAVY, spaceBefore=15, spaceAfter=8, keepWithNext=True)
h2 = ParagraphStyle("H2Custom", parent=styles["Heading2"], fontSize=11, leading=15,
                    textColor=TEAL, spaceBefore=10, spaceAfter=5)
body = ParagraphStyle("BodyCustom", parent=styles["BodyText"], fontName="Helvetica",
                      fontSize=9.2, leading=14, textColor=INK, spaceAfter=8)
small = ParagraphStyle("SmallCustom", parent=body, fontSize=8, leading=11, spaceAfter=3)
cell = ParagraphStyle("CellCustom", parent=body, fontSize=8.1, leading=11, spaceAfter=0)
header_cell = ParagraphStyle("HeaderCell", parent=cell, fontName="Helvetica-Bold", textColor=colors.white)
code_style = ParagraphStyle("CodeCustom", fontName="Courier", fontSize=7.3, leading=10,
                            textColor=INK, leftIndent=8, rightIndent=8)


def para(text, style=body):
    return Paragraph(text, style)


def bullet(text):
    return Paragraph("<font color='#0A8B86'><b>•</b></font>  " + text,
                     ParagraphStyle("bullet", parent=body, leftIndent=12, firstLineIndent=-12))


def section(number, heading):
    return para(f"{number}. {heading}", h1)


def table(headers, rows, widths):
    data = [[para(escape(str(x)), header_cell) for x in headers]]
    data += [[para(str(x), cell) for x in row] for row in rows]
    result = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F5F8FA")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
        ("LINEBELOW", (0, -1), (-1, -1), 0.6, RULE),
    ]))
    return result


class Architecture(Flowable):
    def __init__(self):
        super().__init__()
        self.width = 480
        self.height = 145

    def draw(self):
        c = self.canv
        boxes = [
            ("Builder", "valid setup"), ("Facade", "workflow API"),
            ("Strategy", "choose seat"), ("Factory", "make ticket"),
            ("State", "lifecycle"),
        ]
        x0, width, gap = 0, 84, 15
        for index, (label, detail) in enumerate(boxes):
            x = x0 + index * (width + gap)
            c.setFillColor(PALE if index != 1 else TEAL)
            c.setStrokeColor(TEAL)
            c.roundRect(x, 82, width, 48, 7, fill=1, stroke=1)
            c.setFillColor(INK if index != 1 else colors.white)
            c.setFont("Helvetica-Bold", 9)
            c.drawCentredString(x + width / 2, 108, label)
            c.setFont("Helvetica", 7)
            c.drawCentredString(x + width / 2, 95, detail)
            if index < len(boxes) - 1:
                c.setStrokeColor(MUTED)
                c.line(x + width + 2, 106, x + width + gap - 3, 106)
                c.line(x + width + gap - 3, 106, x + width + gap - 7, 110)
                c.line(x + width + gap - 3, 106, x + width + gap - 7, 102)
        c.setFillColor(NAVY)
        c.setStrokeColor(NAVY)
        c.roundRect(87, 12, 108, 42, 7, fill=1, stroke=1)
        c.setFillColor(colors.white)
        c.setFont("Helvetica-Bold", 9)
        c.drawCentredString(141, 37, "Observer")
        c.setFont("Helvetica", 7)
        c.drawCentredString(141, 24, "seat and price events")
        c.setStrokeColor(MUTED)
        c.line(141, 54, 141, 82)
        c.line(141, 82, 137, 76)
        c.line(141, 82, 145, 76)


class UMLDiagram(Flowable):
    """A compact UML class diagram; the full editable model is in docs/."""

    def __init__(self):
        super().__init__()
        self.width = 480
        self.height = 225

    def draw(self):
        c = self.canv

        def box(x, y, width, height, lines, interface=False):
            c.setFillColor(PALE if interface else colors.white)
            c.setStrokeColor(NAVY)
            c.rect(x, y, width, height, fill=1, stroke=1)
            c.setFillColor(INK)
            for index, line in enumerate(lines):
                c.setFont("Helvetica-Bold" if index == len(lines) - 1 else "Helvetica", 8)
                c.drawCentredString(x + width / 2, y + height - 14 - 12 * index, line)

        def generalization(x):
            c.setStrokeColor(NAVY)
            c.line(x, 97, x, 137)
            path = c.beginPath()
            path.moveTo(x, 137)
            path.lineTo(x - 7, 125)
            path.lineTo(x + 7, 125)
            path.close()
            c.setFillColor(colors.white)
            c.drawPath(path, fill=1, stroke=1)

        columns = [
            (0, ("&lt;&lt;interface&gt;&gt;", "TicketFactory"),
             ("ConcertTicketFactory", "FlightTicketFactory")),
            (165, ("&lt;&lt;interface&gt;&gt;", "SeatSelectionStrategy"),
             ("FrontRowStrategy", "BackRowStrategy")),
            (330, ("&lt;&lt;interface&gt;&gt;", "Observer"),
             ("CustomerNotifier", "AvailabilityBoard")),
        ]
        for x, abstract, concrete in columns:
            # ReportLab canvas draws plain text, so use the UML stereotype directly.
            box(x, 137, 150, 53, ("<<interface>>", abstract[1]), True)
            box(x, 43, 150, 54, concrete)
            generalization(x + 75)
        c.setFillColor(MUTED)
        c.setFont("Helvetica", 8)
        c.drawString(0, 8, "Open triangles show implementation of the interface above each pair.")


class ReportDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(str(filename), pagesize=A4, leftMargin=54, rightMargin=54,
                         topMargin=61, bottomMargin=54, title="Ticket Reservation System - Final Report",
                         author="Natalyev, eslishnah, MangoDeath")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height,
                      leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        self.addPageTemplates(PageTemplate(id="report", frames=frame, onPage=self.decorate))

    def decorate(self, canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(RULE)
        canvas.line(54, A4[1] - 43, A4[0] - 54, A4[1] - 43)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.setFillColor(TEAL)
        canvas.drawString(54, A4[1] - 34, "TICKET RESERVATION SYSTEM")
        canvas.setStrokeColor(RULE)
        canvas.line(54, 42, A4[0] - 54, 42)
        canvas.setFillColor(MUTED)
        canvas.setFont("Helvetica", 8)
        canvas.drawString(54, 29, "Software Design Patterns - Final Project")
        canvas.drawRightString(A4[0] - 54, 29, str(doc.page))
        canvas.restoreState()


story = [Spacer(1, 78), para("Ticket Reservation<br/>System", title), Spacer(1, 12),
         para("Final project report · Java 17 · Six integrated design patterns", subtitle),
         Spacer(1, 55), para("Team", h2),
         para("Natalyev  ·  eslishnah  ·  MangoDeath"),
         Spacer(1, 20), para("Repository", h2),
         para("<link href='https://github.com/Natalyev/sdp-final-' color='#0A8B86'>github.com/Natalyev/sdp-final-</link>"),
         Spacer(1, 20), para("Deliverables", h2),
         para("Source code, responsive browser interface, editable UML, JUnit tests, and this report."),
         PageBreak()]

story += [section(1, "Abstract"),
          para("Ticket Reservation System is an in-memory Java application for concert and flight bookings. "
               "Users register events, book numbered seats, confirm or cancel reservations, change prices, "
               "and switch seating policies. A responsive browser interface shows the seat map, bookings, "
               "and live notifications. The application "
               "integrates Factory Method, Strategy, Observer, Builder, State, and Facade in one workflow."),
          section(2, "Requirements and scenario"),
          para("Each event has its own ticket category, price, seating policy, and available seats. Booking "
               "chooses one available seat and creates the correct ticket. A booking starts pending. "
               "Confirmation and cancellation follow controlled transitions. Cancellation returns a seat. "
               "Invalid IDs, duplicate events, exhausted inventories, and illegal transitions are rejected."),
          para("In the demo, a customer books concert seat 1 using the front-row policy. The reservation "
               "is confirmed. The policy switches to back-row, so the next booking takes seat 3. "
               "Cancellation returns seat 3. A separate flight event then creates a flight ticket."),
          section(3, "Architecture"),
          para("The browser and console both use the Facade. The Facade owns one service and inventory per event. Builder validates registration data. "
               "The service uses Strategy to select a seat and Factory Method to create its ticket. "
               "A reservation session delegates transitions to State objects. Observer distributes seat "
               "and price changes to subscribed views."), Spacer(1, 10), Architecture(), Spacer(1, 8),
          para("Editable UML class diagram: <font name='Courier'>docs/class-diagram.puml</font>.", small),
          PageBreak(), section(4, "UML class diagram"),
          para("The following class relationships show the Factory Method, Strategy, and Observer interfaces "
               "with their concrete implementations. The editable PlantUML file contains the complete "
               "diagram, including Builder, State, Facade, and their associations."),
          Spacer(1, 16), UMLDiagram(),
          PageBreak(), section(5, "Pattern roles and integration")]

pattern_rows = [
    ("Factory Method", "TicketFactory; concert and flight factories", "The service creates a concrete ticket through a factory interface."),
    ("Strategy", "SeatSelectionStrategy; front/back strategies", "The inventory delegates seat choice; the facade can replace the policy at runtime."),
    ("Observer", "TicketEventPublisher; notifier; board", "Subscribed views receive seat-count or price events without service-to-view coupling."),
    ("Builder", "EventConfiguration.Builder", "A complete configuration is validated before an event is registered."),
    ("State", "PendingState; ConfirmedState; CancelledState", "The current state decides whether confirmation or cancellation is allowed."),
    ("Facade", "TicketReservationSystem", "A single API coordinates setup, booking, lifecycle, pricing, subscriptions, and customer history."),
]
story += [table(("Pattern", "Participants", "Purpose"), pattern_rows, (92, 180, 215)),
          section(6, "Java code example"),
          para("The client assembles a complete event, then books and confirms through the Facade:"),
          Preformatted("system.register(new EventConfiguration.Builder(\"SHOW\")\n"
                       "    .factory(new ConcertTicketFactory())\n"
                       "    .strategy(new FrontRowStrategy())\n"
                       "    .seats(Set.of(1, 2, 3))\n"
                       "    .price(new BigDecimal(\"50.00\")).build());\n"
                       "Reservation booking = system.book(\"SHOW\", \"CUSTOMER-1\");\n"
                       "system.confirm(booking.id());", code_style),
          section(7, "Design decisions"),
          bullet("Separate inventories prevent concert and flight seats from interfering."),
          bullet("Immutable reservation snapshots keep lifecycle changes inside the facade, where seat release also occurs."),
          bullet("The inventory removes a chosen seat atomically and rejects an unavailable strategy result."),
          bullet("If ticket creation fails, the selected seat is returned before the error is propagated."),
          bullet("Synchronous Observer delivery and CopyOnWriteArrayList allow subscription changes during notification."),
          section(8, "Verification"),
          para("<b>20 tests passed · 0 failures · 0 errors.</b> Ten tests cover the initial patterns and service "
               "integration. Eight workflow tests cover all-pattern integration, Builder validation, "
               "state transitions, seat reuse, independent events, interactive commands, rollback, and "
               "per-event availability counts. One Facade test covers customer reservation lookup. One HTTP "
               "test exercises the browser workflow through the same Facade."),
          para("Actual terminal run evidence is provided in the three text files under <font name='Courier'>evidence/</font>."),
          Preformatted("Tests run: 20, Failures: 0, Errors: 0, Skipped: 0\nBUILD SUCCESS", code_style),
          section(9, "How to run"),
          Preformatted("mvn clean test\nmvn exec:java\nmvn package\njava -cp target/classes tickets.Main --web\njava -cp target/classes tickets.Main --interactive", code_style),
          para("Web mode serves the browser interface at <font name='Courier'>http://127.0.0.1:8080</font> "
               "on the local computer. It starts with 30 concert seats and 18 flight seats."),
          para("Interactive commands: <font name='Courier'>help</font>, <font name='Courier'>events</font>, "
               "<font name='Courier'>book EVENT CUSTOMER</font>, <font name='Courier'>confirm ID</font>, "
               "<font name='Courier'>cancel ID</font>, <font name='Courier'>show ID</font>, "
               "<font name='Courier'>price EVENT AMOUNT</font>, "
               "<font name='Courier'>strategy EVENT front|back</font>, and <font name='Courier'>quit</font>."),
          section(10, "Limitations and extensions"),
          para("The application is in-memory: data is lost when it exits. It does not process payments, "
               "authenticate accounts, allow manual selection of a specific seat, or book multiple seats "
               "in one order. A database and payment flow are natural future extensions."),
          section(11, "Execution evidence"),
          para("The following excerpts come from actual runs of the current code. Full transcripts are "
               "included in the <font name='Courier'>evidence/</font> folder."),
          para("Demo run", h2),
          Preformatted("Booked concert seat: 1 (PENDING)\n"
                       "Confirmed reservation: R-1 (CONFIRMED)\n"
                       "After strategy switch, seat: 3\n"
                       "Cancelled reservation; concert seats available: 2\n"
                       "Flight ticket: FlightTicket[eventId=FLIGHT-1, seat=11, price=200.00]",
                       code_style),
          para("Interactive run", h2),
          Preformatted("CONCERT-1: 3 seats available\n"
                       "EVENT SEAT_AVAILABILITY_CHANGED CONCERT-1 seats=2 price=50.00\n"
                       "R-1 PENDING ConcertTicket[eventId=CONCERT-1, seat=1, price=50.00]\n"
                       "R-1 CONFIRMED ConcertTicket[eventId=CONCERT-1, seat=1, price=50.00]\n"
                       "R-1 CANCELLED ConcertTicket[eventId=CONCERT-1, seat=1, price=50.00]\n"
                       "CONCERT-1: 3 seats available", code_style),
          section(12, "Conclusion"),
          para("The prototype demonstrates the requested Ticket Reservation System topic. Six patterns "
               "cooperate in event setup, seat selection, ticket creation, reservation transitions, and "
               "notifications. The responsive browser interface makes their effects visible during the defense. "
               "The Maven build succeeds and all 20 tests pass."),
          section(13, "Further work"),
          para("Persistent storage, payment processing, multi-seat orders, authentication, and manual "
               "seat selection would extend the prototype for broader use."),
          section(14, "Team and repository"),
          para("The repository records work under the GitHub handles Natalyev, eslishnah, and MangoDeath. "
               "Its commit history is the source for exact file-level attribution. Full names can be "
               "provided in the course submission form if required."),
          para("Repository: <link href='https://github.com/Natalyev/sdp-final-' color='#0A8B86'>"
               "https://github.com/Natalyev/sdp-final-</link>.")]

ReportDoc(OUTPUT).build(story)
print(OUTPUT)
