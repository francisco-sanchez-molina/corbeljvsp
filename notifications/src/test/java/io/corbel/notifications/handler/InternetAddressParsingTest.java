package io.corbel.notifications.handler;

import org.junit.Assert;
import org.junit.Test;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public class InternetAddressParsingTest {

    @Test
    public void testParse() throws AddressException {
        InternetAddress[] addresses = InternetAddress.parse("Test User <test@example.org>");
        Assert.assertEquals(1, addresses.length);
        InternetAddress address = addresses[0];
        Assert.assertEquals("Test User", address.getPersonal());
        Assert.assertEquals("test@example.org", address.getAddress());
    }

    @Test
    public void testParseAddressOnly() throws AddressException {
        InternetAddress[] addresses = InternetAddress.parse("test@example.org");
        Assert.assertEquals(1, addresses.length);
        InternetAddress address = addresses[0];
        Assert.assertNull(address.getPersonal());
        Assert.assertEquals("test@example.org", address.getAddress());

    }

}
