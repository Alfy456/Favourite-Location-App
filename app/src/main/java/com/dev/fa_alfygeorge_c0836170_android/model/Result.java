package com.dev.fa_alfygeorge_c0836170_android.model;

import java.util.List;

public class Result {
    private List<String> destination_address;
    private List<String> origin_address;
    private List<Rows> rows;
    private String status;

    public List<String> getDestination_address() {
        return destination_address;
    }

    public void setDestination_address(List<String> destination_address) {
        this.destination_address = destination_address;
    }

    public List<String> getOrigin_address() {
        return origin_address;
    }

    public void setOrigin_address(List<String> origin_address) {
        this.origin_address = origin_address;
    }

    public List<Rows> getRows() {
        return rows;
    }

    public void setRows(List<Rows> rows) {
        this.rows = rows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public class Distance{
        private String text;
        private int value;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public class Duration{
        private String text;
        private int value;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public class  Elements{
        private Distance distance;
        private Duration duration;
        private String status;

        public Distance getDistance() {
            return distance;
        }

        public void setDistance(Distance distance) {
            this.distance = distance;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
    public class Rows{
        private List<Elements> elements;

        public List<Elements> getElements() {
            return elements;
        }

        public void setElements(List<Elements> elements) {
            this.elements = elements;
        }
    }
}
