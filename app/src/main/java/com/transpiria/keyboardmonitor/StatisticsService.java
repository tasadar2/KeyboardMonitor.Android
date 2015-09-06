package com.transpiria.keyboardmonitor;

import android.telephony.SubscriptionManager;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StatisticsService
        implements
        com.transpiria.keyboardmonitor.Communicator.IEndpointDiscovered,
        com.transpiria.keyboardmonitor.Communicator.IMessageReceived {

    public interface ISubscriptionsChanged {
        void SubscriptionsChanged();
    }

    private Communicator Communicator;
    private Subscription Subscription;
    private Subscription LastSubscription;

    public List<Subscription> Subscriptions;

    public final Event<ISubscriptionsChanged> SubscriptionsChangedEvent = new Event<>();

    public StatisticsService() {
        Subscriptions = new ArrayList<>();

        Communicator = new Communicator();
        Communicator.EndpointDiscoveredEvent.AddObserver(this);
        Communicator.IMessageReceivedEvent.AddObserver(this);
        Communicator.Discover(27831);
    }

    public boolean isSubscribed() {
        return Subscription != null;
    }

    public void Subscribe() {
        Subscribe(LastSubscription, false);
    }

    public void Subscribe(Subscription subscription, boolean resubscribe) {
        if (subscription != Subscription || resubscribe) {
            UnSubscribe();
        }

        if (subscription != null && !isSubscribed()) {
            LastSubscription = Subscription = subscription;
            Communicator.Subscribe(subscription.Address, subscription.Port);
        }
    }

    public void UnSubscribe() {
        if (isSubscribed()) {
            Communicator.Unsubscribe(Subscription.Address, Subscription.Port);
            Subscription = null;
        }
    }

    @Override
    public void EndpointDiscovered(InetAddress ipAddress, int port) {
        Subscription newSubscription = null;
        long now = new Date().getTime();
        long subscriptionExpiration = 5 * 60 * 1000;
        boolean subscriptionsChanged = false;
        for (Subscription subscription : Subscriptions) {
            if (subscription.Address == ipAddress && subscription.Port == port) {
                newSubscription = subscription;
            } else if (now - subscription.LastSeen.getTime() > subscriptionExpiration) {
                Subscriptions.remove(subscription);
                subscriptionsChanged = true;
            }
        }

        if (newSubscription == null) {
            newSubscription = new Subscription();
            newSubscription.Address = ipAddress;
            newSubscription.Port = port;
            Subscriptions.add(newSubscription);
            subscriptionsChanged = true;
        }

        newSubscription.LastSeen = new Date();
        newSubscription.LastReceived = new Date();

        if (subscriptionsChanged) {
            OnSubscriptionsChanged();
        }
    }

    protected void OnSubscriptionsChanged() {
        for (ISubscriptionsChanged observer : SubscriptionsChangedEvent.Observers) {
            observer.SubscriptionsChanged();
        }
    }

    @Override
    public void MessageReceived(byte[] content) {
        try {
            if (LastSubscription != null) {
                LastSubscription.LastReceived = new Date();
            }

            final Info info = new Info();
            String jsonText = new String(content, "UTF-8");
            JSONObject json = new JSONObject(jsonText);

            info.Processor = GetCounter(json.getJSONObject("Processors"));
            info.BytesReceived = GetCounter(json.getJSONObject("BytesReceived"));
            info.BytesSent = GetCounter(json.getJSONObject("BytesSent"));

            Current = info;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Info Current;

    private Counter GetCounter(JSONObject json) {
        Counter counter = new Counter();
        try {
            counter.Name = json.getString("Name");
            counter.Value = json.getDouble("Value");
            JSONArray values = json.getJSONArray("Values");
            for (int index = 0; index < values.length(); index++) {
                counter.Values.add(values.getDouble(index));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return counter;
    }

    public class Info {
        public Counter Processor;
        public Counter BytesReceived;
        public Counter BytesSent;
    }

    public class Counter {
        public String Name;
        public double Value;
        public List<Double> Values = new ArrayList<>();
    }

    public class Subscription {
        public String Name;
        public InetAddress Address;
        public int Port;
        public Date LastSeen;
        public Date LastReceived;
    }

}
