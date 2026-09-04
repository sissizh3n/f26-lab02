package edu.cmu.cs214.availability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based tests for {@link AvailabilityCalculator}.
 *
 * <p>One example property is provided below: it checks that no returned free slot
 * overlaps a booking, and it passes. In Milestone 1 you add a stronger property
 * that pins down what "correct availability" actually means. See the lab handout.
 */
class AvailabilityProperties {

    private final AvailabilityCalculator calc = new AvailabilityCalculator();

    /** Provided example: every returned free slot is genuinely free (overlaps no booking). */
    @Property
    void freeSlotsNeverOverlapABooking(@ForAll("scenarios") Scenario s) {
        List<TimeInterval> free = calc.freeSlots(s.dayStart(), s.dayEnd(), s.bookings());
        for (TimeInterval slot : free) {
            for (TimeInterval booking : s.bookings()) {
                assertFalse(slot.overlaps(booking),
                    () -> "free slot " + slot + " overlaps booking " + booking);
            }
        }
    }

    // --- Milestone 1: add your stronger property here ---

    /**
     * Every minute of the business day is either covered by a booking or reported free.
     * Never both never neither.
     */
    @Property
    void everyMinuteIsEitherBookedOrFree(@ForAll("scenarios") Scenario s) {
        List<TimeInterval> free = calc.freeSlots(s.dayStart(), s.dayEnd(), s.bookings());
        for (int m = s.dayStart(); m < s.dayEnd(); m++) {
            TimeInterval minute = new TimeInterval(m, m + 1);
            boolean booked = covers(s.bookings(), minute);
            boolean reportedFree = covers(free, minute);
            assertTrue(booked != reportedFree,
                () -> "minute " + minute + " is "
                    + (booked ? "both booked and reported free"
                              : "neither booked nor reported free"));
        }
    }

    /** Does any interval in {@code intervals} cover the one-minute interval {@code minute}? */
    private static boolean covers(List<TimeInterval> intervals, TimeInterval minute) {
        for (TimeInterval i : intervals) {
            if (i.overlaps(minute)) {
                return true;
            }
        }
        return false;
    }

    /** Generates a business day plus a list of bookings (possibly unsorted, overlapping, or outside hours). */
    // every minute is either free or covered by a booking
    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Integer> minutes = Arbitraries.integers().between(0, 1440);
        Arbitrary<TimeInterval> intervals = Combinators.combine(minutes, minutes)
            .as((a, b) -> new TimeInterval(Math.min(a, b), Math.max(a, b) + 1));
        Arbitrary<List<TimeInterval>> bookings = intervals.list().ofMaxSize(6);
        return Combinators.combine(minutes, minutes, bookings)
            .as((a, b, bk) -> new Scenario(Math.min(a, b), Math.max(a, b) + 1, bk));
    }

    record Scenario(int dayStart, int dayEnd, List<TimeInterval> bookings) {
    }
}
