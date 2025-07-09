package com.ancienty.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

public class InvoiceUploadData {

    @JacksonXmlRootElement(localName = "uploadSystem")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadSystem {
        @JsonProperty("customer")
        @JacksonXmlProperty(localName = "customer")
        public Customer customer;

        @JsonProperty("invoiceData")
        @JacksonXmlProperty(localName = "invoiceData")
        public InvoiceData invoiceData;

        public UploadSystem() {}

        public UploadSystem(Customer customer, InvoiceData invoiceData) {
            this.customer = customer;
            this.invoiceData = invoiceData;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        @JsonProperty("name")
        @JacksonXmlProperty(localName = "name")
        public String name;

        @JsonProperty("ssn")
        @JacksonXmlProperty(localName = "ssn")
        public String ssn;

        @JsonProperty("type")
        @JacksonXmlProperty(localName = "type")
        public String type;

        public Customer() {}

        public Customer(String name, String ssn, String type) {
            this.name = name;
            this.ssn = ssn;
            this.type = type;
        }

        @JsonIgnore
        public boolean isCompany() {
            return "COMPANY".equals(type) || "SIRKET".equals(type);
        }

        @JsonIgnore
        public long ssnNumber() {
            try {
                return Long.parseLong(ssn);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceData {
        @JsonProperty("seri")
        @JacksonXmlProperty(localName = "seri")
        public String seri;

        @JsonProperty("number")
        @JacksonXmlProperty(localName = "number")
        public String number;

        @JsonProperty("items")
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Item> items;

        @JsonProperty("totalAmount")
        @JacksonXmlProperty(localName = "totalAmount")
        public double totalAmount;

        @JsonProperty("discount")
        @JacksonXmlProperty(localName = "discount")
        public double discount;

        @JsonProperty("amountToPay")
        @JacksonXmlProperty(localName = "amountToPay")
        public double amountToPay;

        public InvoiceData() {}

        public InvoiceData(String seri, String number, List<Item> items, double totalAmount, double discount, double amountToPay) {
            this.seri = seri;
            this.number = number;
            this.items = items;
            this.totalAmount = totalAmount;
            this.discount = discount;
            this.amountToPay = amountToPay;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("name")
        @JacksonXmlProperty(localName = "name")
        public String name;

        @JsonProperty("quantity")
        @JacksonXmlProperty(localName = "quantity")
        public double quantity;

        @JsonProperty("unitPrice")
        @JacksonXmlProperty(localName = "unitPrice")
        public double unitPrice;

        @JsonProperty("lineTotal")
        @JacksonXmlProperty(localName = "lineTotal")
        public double lineTotal;

        public Item() {}

        public Item(String name, double quantity, double unitPrice, double lineTotal) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }
    }
} 