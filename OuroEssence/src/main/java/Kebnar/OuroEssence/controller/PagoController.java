package Kebnar.OuroEssence.controller;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PagoController {

    @Autowired
    private APIContext apiContext;

    @Value("${server.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/pay")
    public RedirectView crearPago(@RequestParam("nombre") String nombre,
                              @RequestParam("precio") String precio,
                              @RequestParam("cantidad") int cantidad,
                              @RequestParam("sku") String sku) {
        // Construir el ítem con los datos recibidos
        Item item = new Item();
        item.setName(nombre);
        item.setSku(sku);
        item.setPrice(precio);
        item.setCurrency("MXN");
        item.setQuantity(String.valueOf(cantidad));

        ItemList itemList = new ItemList();
        List<Item> items = new ArrayList<>();
        items.add(item);
        itemList.setItems(items);

       Amount amount = new Amount();
       amount.setCurrency("MXN");
       amount.setTotal(precio);
       
       Transaction transaction = new Transaction();
       transaction.setDescription("Compra de " + nombre);
       transaction.setAmount(amount);
       transaction.setItemList(itemList);
       
       List<Transaction> transactions = new ArrayList<>();
       transactions.add(transaction);

    
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

    
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(baseUrl + "/cancel");
        redirectUrls.setReturnUrl(baseUrl + "/success");

    
        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setRedirectUrls(redirectUrls);

        try {
            Payment createdPayment = payment.create(apiContext);
            for (Links links : createdPayment.getLinks()) {
                if (links.getRel().equals("approval_url")) {
                    return new RedirectView(links.getHref());
                }
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
            return new RedirectView("/pago-error");
        }
        return new RedirectView("/pago-cancelado");
    }

    @GetMapping("/success")
    public String ejecutarPago(@RequestParam("paymentId") String paymentId,
                               @RequestParam("PayerID") String payerId) {
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        try {
            Payment executedPayment = payment.execute(apiContext, paymentExecution);
            if (executedPayment.getState().equals("approved")) {
                return "pago-exitoso";  // Nombre de una vista HTML (pago-exitoso.html)
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
        }
        return "pago-error";
    }

    @GetMapping("/cancel")
    public String cancelarPago() {
        return "pago-cancelado";
    }
}