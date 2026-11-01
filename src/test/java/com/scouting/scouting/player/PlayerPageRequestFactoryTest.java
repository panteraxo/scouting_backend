package com.scouting.scouting.player;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.player.service.PlayerPageRequestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerPageRequestFactoryTest {

    @Test
    void defaultSortIsRecordRatingDescendingWithLastNameTieBreak() {
        Pageable pageable = PlayerPageRequestFactory.create(0, 20, null);

        List<Sort.Order> orders = orders(pageable);
        assertEquals(4, orders.size());
        assertEquals("recordRating", orders.get(0).getProperty());
        assertEquals(Sort.Direction.DESC, orders.get(0).getDirection());
        assertEquals("user.lastName", orders.get(1).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(1).getDirection());
        assertEquals("user.firstName", orders.get(2).getProperty());
        assertEquals("id", orders.get(3).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(3).getDirection());
    }

    @Test
    void blankSortAlsoUsesRecordDefault() {
        Pageable pageable = PlayerPageRequestFactory.create(0, 20, "  ");

        assertEquals("recordRating", orders(pageable).get(0).getProperty());
    }

    @Test
    void recordSortIsAllowedAndKeepsTieBreak() {
        Pageable pageable = PlayerPageRequestFactory.create(0, 20, "record,asc");

        List<Sort.Order> orders = orders(pageable);
        assertEquals("recordRating", orders.get(0).getProperty());
        assertEquals(Sort.Direction.ASC, orders.get(0).getDirection());
        assertEquals("user.lastName", orders.get(1).getProperty());
    }

    @Test
    void unknownSortPropertyIsRejected() {
        assertThrows(ApiException.class, () -> PlayerPageRequestFactory.create(0, 20, "secretField,asc"));
    }

    private List<Sort.Order> orders(Pageable pageable) {
        return StreamSupport.stream(pageable.getSort().spliterator(), false).toList();
    }
}
