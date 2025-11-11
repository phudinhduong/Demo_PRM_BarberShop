//package com.he187383.barber.service;
//
//
//import com.he187383.barber.entity.*;
//import com.he187383.barber.repo.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//
//@Service
//@RequiredArgsConstructor
//public class OwnerService {
//    private final BarberRepository barberRepo;
//    private final ServiceItemRepository serviceRepo;
//    private final WorkShiftRepository shiftRepo;
//    private final TimeOffRepository timeOffRepo;
//
//    // BARBER
//    @Transactional public Barber addBarber(String name, String bio, String avatarUrl, Boolean active){
//        Barber b = new Barber();
//        b.setName(name);
//        b.setBio(bio);
//        b.setAvatarUrl(avatarUrl);
//        b.setIsActive(active != null ? active : true);
//        return barberRepo.save(b);
//    }
//    @Transactional public Barber updateBarber(Long id, String name, String bio, String avatarUrl, Boolean active){
//        Barber b = barberRepo.findById(id).orElseThrow();
//        if (name != null) b.setName(name);
//        if (bio != null) b.setBio(bio);
//        if (avatarUrl != null) b.setAvatarUrl(avatarUrl);
//        if (active != null) b.setIsActive(active);
//        return barberRepo.save(b);
//    }
//
//    // SERVICE
//    @Transactional public ServiceItem addService(String name, Integer durationMin, Integer price, Boolean active){
//        ServiceItem s = new ServiceItem();
//        s.setName(name); s.setDurationMin(durationMin); s.setPrice(price);
//        s.setIsActive(active != null ? active : true);
//        return serviceRepo.save(s);
//    }
//    @Transactional public ServiceItem updateService(Long id, String name, Integer durationMin, Integer price, Boolean active){
//        ServiceItem s = serviceRepo.findById(id).orElseThrow();
//        if (name!=null) s.setName(name);
//        if (durationMin!=null) s.setDurationMin(durationMin);
//        if (price!=null) s.setPrice(price);
//        if (active!=null) s.setIsActive(active);
//        return serviceRepo.save(s);
//    }
//
//    // SHIFT — thêm 1 ca
//    @Transactional public WorkShift addShift(Long barberId, Integer weekday, String startHHmm, String endHHmm){
//        Barber b = barberRepo.findById(barberId).orElseThrow();
//        WorkShift w = new WorkShift();
//        w.setBarber(b);
//        w.setWeekday(weekday);
//        w.setStartTime(LocalTime.parse(startHHmm));
//        w.setEndTime(LocalTime.parse(endHHmm));
//        return shiftRepo.save(w);
//    }
//    @Transactional public void deleteShift(Long shiftId){ shiftRepo.deleteById(shiftId); }
//
//    // TIME OFF
//    @Transactional public TimeOff addTimeOff(Long barberId, String startIso, String endIso, String reason){
//        Barber b = barberRepo.findById(barberId).orElseThrow();
//        TimeOff t = new TimeOff();
//        t.setBarber(b);
//        t.setStartDt(LocalDateTime.parse(startIso));
//        t.setEndDt(LocalDateTime.parse(endIso));
//        t.setReason(reason);
//        return timeOffRepo.save(t);
//    }
//    @Transactional public void deleteTimeOff(Long id){ timeOffRepo.deleteById(id); }
//}
