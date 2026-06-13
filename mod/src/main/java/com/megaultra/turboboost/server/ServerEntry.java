package com.megaultra.turboboost.server;

/** One saved server in the smart switcher list (mirrors shared/servers.example.json). */
public class ServerEntry {
    public String name;
    public String address;
    public String category;
    public String notes;

    public ServerEntry() {}

    public ServerEntry(String name, String address, String category, String notes) {
        this.name = name;
        this.address = address;
        this.category = category;
        this.notes = notes;
    }
}
