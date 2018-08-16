package com.baasbox.databean;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class DailyStatsBean {

    private ODocument user;

    private Date collectionDate;
    private Integer onlineTime;
    private Double earning;
    private Integer totalTrips;

    public ODocument getUser() {
        return user;
    }
    
    public void setUser(ODocument user) {
        this.user = user;
    }
    
    public Date getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Date collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Integer getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(Integer onlineTime) {
        this.onlineTime = onlineTime;
    }

    public Double getEarning() {
        return earning;
    }

    public void setEarning(Double earning) {
        this.earning = earning;
    }

    public Integer getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("collectionDate", collectionDate).append("onlineTime", onlineTime).append("earning", earning).append("totalTrips", totalTrips).toString();
    }

}