package com.vaka.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Iaroslav on 11/23/2016.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bill extends BaseEntity {
    private Reservation reservation;
    private Integer totalCost;
    private boolean paid;
}
