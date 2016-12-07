package com.vaka.util;

import com.vaka.domain.Bill;
import com.vaka.domain.Reservation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created by Iaroslav on 12/6/2016.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainFactory {

    public static Bill createBillFromReservation(Reservation reservation) {
        if (reservation.getRoom() == null)
            throw new IllegalStateException("Reservation room cannot be null");
        Bill bill = new Bill();
        bill.setReservation(reservation);
        bill.setTotalCost((int) (reservation.getRoom().getCostPerDay() * (reservation.getDepartureDate().toEpochDay() - reservation.getArrivalDate().toEpochDay())));
        return bill;
    }
}
