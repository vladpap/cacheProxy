package ru.sbt.cacheproxy;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;
import ru.sbt.service.Service;
import ru.sbt.service.ServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class CacheProxyTest {
    @Mock
    ServiceImpl serviceTest;

    @Test
    public void dependencyIsNotCalled() {

        CacheProxy cacheProxy = new CacheProxy();
        final Service service = cacheProxy.cache(serviceTest);
        verify(service, never()).doHardWork("sdfsf", 34);
        service.doHardWork("sdfsf", 34);
        verify(serviceTest, times(1)).doHardWork("sdfsf", 34);
    }
}