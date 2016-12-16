package com.vaka.hotel_manager.web.controller;

import com.vaka.hotel_manager.context.ApplicationContext;
import com.vaka.hotel_manager.domain.Reservation;
import com.vaka.hotel_manager.domain.ReservationStatus;
import com.vaka.hotel_manager.domain.Role;
import com.vaka.hotel_manager.domain.User;
import com.vaka.hotel_manager.service.ReservationService;
import com.vaka.hotel_manager.service.RoomService;
import com.vaka.hotel_manager.service.SecurityService;
import com.vaka.hotel_manager.service.UserService;
import com.vaka.hotel_manager.util.IntegrityUtil;
import com.vaka.hotel_manager.util.ServletToDomainExtractor;
import com.vaka.hotel_manager.util.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Created by Iaroslav on 12/1/2016.
 */
public class ReservationController {
    private ReservationService reservationService;
    private UserService userService;
    private RoomService roomService;
    private SecurityService securityService;

    private static final Logger LOG = LoggerFactory.getLogger(ReservationController.class);

    public void create(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("Creating reservation request");
        Reservation reservation = ServletToDomainExtractor.extractReservation(req);
        IntegrityUtil.check(reservation);
        if (loggedUser.getRole() == Role.ANONYMOUS) {
            //if user isn't authenticated - trying to register by given info
            LOG.debug("Creating user for reservation");
            User created = ServletToDomainExtractor.extractUser(req);
            created.setPassword(created.getPhoneNumber());
            IntegrityUtil.check(created);
            created = getUserService().create(loggedUser, created);
            reservation.setUser(created);
            getSecurityService().signIn(req.getSession(), created.getEmail(), created.getPhoneNumber());
        } else {
            reservation.setUser(loggedUser);
        }
        reservation = getReservationService().create(loggedUser, reservation);
        LOG.debug("To reservation page, reservationId: {}", reservation.getId());
        resp.sendRedirect("/reservations/" + reservation.getId());
    }

    public void getById(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("To reservation page");
        if (loggedUser.getRole() == Role.ANONYMOUS) {
            resp.sendRedirect("/signin");
            return;
        }
        try {
            Integer reservationId = Integer.valueOf(req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1));
            Optional<Reservation> reservation = getReservationService().getById(loggedUser, reservationId);
            if (!reservation.isPresent()) {
                throw new NotFoundException("There are no user with given ID");
            }
            req.setAttribute("reservation", reservation.get());
            req.getRequestDispatcher("/reservationInfo.jsp").forward(req, resp);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("ID can be only positive integer value");
        }
    }

    public void applyRoomForReservation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("Trying to apply room for reservation request");
        if (loggedUser.getRole() == Role.ANONYMOUS) {
            resp.sendRedirect("/signin");
            return;
        }
        Integer reservationId;
        Integer roomId;
        try {
            reservationId = Integer.valueOf(req.getParameter("reservationId"));
            roomId = Integer.valueOf(req.getParameter("roomId"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID can be only integer value");
        }
        boolean success = getReservationService().applyRoom(loggedUser, roomId, reservationId);//TODO consider how to handle illegal state
        if (!success) {
            req.setAttribute("reservation", getReservationService().getById(loggedUser, reservationId));
            req.setAttribute("rooms", getRoomService().findAvailableForReservation(loggedUser, reservationId));
            req.setAttribute("exception", "Can't apply given room");
            req.getRequestDispatcher("/reservationInfo.jsp").forward(req, resp);
        }
        LOG.debug("Redirect to reservation page, reservationId: {}", reservationId);
        resp.sendRedirect("/reservations/" + reservationId);
    }

    public void confirmedList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("To confirmed reservations page");
        req.setAttribute("reservationList", getReservationService().findByStatusFromDate(loggedUser, ReservationStatus.CONFIRMED, LocalDate.now()));
        req.getRequestDispatcher("/confirmedReservations.jsp").forward(req, resp);
    }

    public void requestsList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("To requested reservations page");
        req.setAttribute("reservationList", getReservationService().findByStatusFromDate(loggedUser, ReservationStatus.REQUESTED, LocalDate.MIN));
        req.getRequestDispatcher("/reservationRequests.jsp").forward(req, resp);
    }

    public void processingPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("Processing reservation request page");
        Integer reservationId = Integer.valueOf(req.getRequestURI().substring(req.getRequestURI().lastIndexOf('/') + 1));
        Optional<Reservation> reservation = getReservationService().getById(loggedUser, reservationId);
        if (!reservation.isPresent()) {
            throw new NotFoundException("Reservation not found");
        }
        req.setAttribute("reservation", reservation.get());
        req.setAttribute("rooms", getRoomService().findAvailableForReservation(loggedUser, reservationId));
        req.getRequestDispatcher("/reservationProcessing.jsp").forward(req, resp);
    }

    public void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User loggedUser = getSecurityService().authenticate(req.getSession());
        LOG.debug("Reservation reject request");
        Integer reservationId = Integer.valueOf(req.getRequestURI().split("/")[2]);
        getReservationService().reject(loggedUser, reservationId);
        LOG.debug("Redirecting to reservation page, reservationId: {}", reservationId);
        resp.sendRedirect("/reservations/" + reservationId);
    }


    public UserService getUserService() {
        if (userService == null) {
            synchronized (this) {
                if (userService == null) {
                    userService = ApplicationContext.getInstance().getBean(UserService.class);
                }
            }
        }
        return userService;
    }

    public SecurityService getSecurityService() {
        if (securityService == null) {
            synchronized (this) {
                if (securityService == null) {
                    securityService = ApplicationContext.getInstance().getBean(SecurityService.class);
                }
            }
        }
        return securityService;
    }

    public ReservationService getReservationService() {
        if (reservationService == null) {
            synchronized (this) {
                if (reservationService == null) {
                    reservationService = ApplicationContext.getInstance().getBean(ReservationService.class);
                }
            }
        }
        return reservationService;
    }

    public RoomService getRoomService() {
        if (roomService == null) {
            synchronized (this) {
                if (roomService == null) {
                    roomService = ApplicationContext.getInstance().getBean(RoomService.class);
                }
            }
        }
        return roomService;
    }
}
