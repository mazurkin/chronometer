Framework similar to java.time.Clock that allows to control time flow in an application and tests.

```java
public class TimeDependent {
    
    private final Chronometer chronometer;
    
    public TimeDependent(Chronometer chronometer) {
        this.chronometer = chronometer;
    }
    
    public void knock() {
        Instant now = chronometer.getInstant();  
        System.out.println("Current time: " + now);
    }
}
```

On production:
```java
public class Main {
    
    public static void main(String[] args) throws Exception {
        TimeDependent d = new TimeDependent(SystemChronometer.INSTANCE);
        d.knock();
    }
}

```

In test:
```java
public class Test {
    
    private MockChronometer chronometer;
    
    private TimeDependent d;
    
    @Before
    public void setUp() throws Exception {
        chronometer = MockChronometer.createFrozen("2017-04-21 14:22:12.000 Europe/Moscow", System.nanoTime());
        d = new TimeDependent(chronometer);
    }
    
    @Test
    public void test() {
        d.knock();
        chronometer.shiftBy(+1, TimeUnit.HOURS);
        d.knock();
        chronometer.shiftTo("2017-04-26 12:33:42.000 UTC");
        d.knock();
        chronometer.setMode(Mode.SYSTEM);
        d.knock();
    }
}
```
