package com.hvn.velocity.controllers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hvn.velocity.entities.Customer;
import com.hvn.velocity.entities.CustomerOrder;
import com.hvn.velocity.entities.Product;
import com.hvn.velocity.services.CategoryService;
import com.hvn.velocity.services.CustomerService;
import com.hvn.velocity.services.OrderService;
import com.hvn.velocity.services.OrderedProductService;
import com.hvn.velocity.services.ProductService;
import com.hvn.velocity.session.Cart;

@Controller
public class FrontStoreController {

	@Autowired
	private Cart cart;
	
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private OrderedProductService orderedProductService;
	
	@Autowired
	private OrderService orderService;

	/*
	 * Category
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(ModelMap mm) {
		mm.put("categoryList", categoryService.listAll());
		return "home";
	}
	
	@RequestMapping(value = "/category", method = RequestMethod.GET)
	public String category(@RequestParam("id") Byte id, ModelMap mm) {
		mm.put("productList", productService.listByCategoryId(id));
		mm.put("categoryList", categoryService.listAll());
		mm.put("id", id);
		return "category";
	}
	
	/*
	 * CRUD on shopping cart
	 */
	@RequestMapping(value = "/cart", method = RequestMethod.GET)
	public String cart(ModelMap mm) {
		Map<Product, Integer> itemMap = cart.getItems();
		double subTotal = cart.calculateSubTotal();
		Integer numOfItems = cart.sumQuantity();
		mm.put("itemMap", itemMap);
		mm.put("numOfItems", numOfItems);
		mm.put("subTotal", subTotal);
		return "cart";
	}

	@RequestMapping(value = "/addToCart", method = RequestMethod.GET)
	public String addToCart(@RequestParam("id") Integer id,
			@RequestHeader("referer") String referer) {
		Product product = productService.getById(id);
		cart.addItem(product);
		return "redirect:" + referer;
	}

	@RequestMapping(value = "/clearCart", method = RequestMethod.GET)
	public String clearCart() {
		cart.clear();
		return "redirect:/cart";
	}

	@RequestMapping(value = "/updateCart", method = RequestMethod.POST)
	public String updateCart(@RequestParam("id") Integer id,
			@RequestParam("quantity") Integer quantity) {
		cart.updateItem(id, quantity);
		return "redirect:/cart";
	}
	
	/*
	 * Checkout
	 */
	@RequestMapping(value = "/checkout", method = RequestMethod.GET)
	public String checkout(ModelMap mm) {
		double subTotal = cart.calculateSubTotal();
		if (subTotal == 0) {
			mm.put("subTotal", subTotal);
		} else {
			double total = cart.calculateTotal(subTotal);
			Map<String, String> cityRegion = new LinkedHashMap<String, String>();
			cityRegion.put("PG", "Prague");
			cityRegion.put("BN", "Benarl");
			cityRegion.put("HS", "Hassen");
			mm.put("subTotal", subTotal);
			mm.put("total", total);
			mm.put("customer", new Customer());
			mm.put("cityRegion", cityRegion);
		}
		return "checkout";
	}
	
	@RequestMapping(value = "/purchaseGuest", method = RequestMethod.POST)
    public String purchaseMember(@ModelAttribute("customer") Customer customer, ModelMap mm) {
            // 1. Save customer (customer)
            Integer customerId = customerService.save(customer);
            
            // 2. Save order (customer_order)
            double subTotal = cart.calculateSubTotal();
            double total = cart.calculateTotal(subTotal);
            Integer orderId = orderService.save(customerId, total);
            
            // 3. Save order details (ordered_product)
            CustomerOrder order = orderService.getById(orderId);
            Map<Product, Integer> itemMap = new HashMap<Product, Integer>(cart.getItems());
            orderedProductService.save(order, itemMap);
            
            mm.put("subTotal", subTotal);
            mm.put("total", total);
            mm.put("order", order);
            mm.put("customer", customer);
            mm.put("itemMap", itemMap);
            
            cart.clear();
            return "confirmation";
    }
    
    @RequestMapping(value = "/purchaseMember", method = RequestMethod.POST)
    public String purchaseGuest(@RequestParam("email") String email, ModelMap mm) {
            // 1. Get id
            Customer customer = customerService.getByEmail(email);
            Integer customerId = customer.getId();

            // 2. Save order (customer_order)
            double subTotal = cart.calculateSubTotal();
            double total = cart.calculateTotal(subTotal);
            Integer orderId = orderService.save(customerId, total);

            // 3. Save order details (ordered_product)
            CustomerOrder order = orderService.getById(orderId);
            Map<Product, Integer> itemMap = new HashMap<Product, Integer>(cart.getItems());
            orderedProductService.save(order, itemMap);

            mm.put("subTotal", subTotal);
            mm.put("total", total);
            mm.put("order", order);
            mm.put("customer", customer);
            mm.put("itemMap", itemMap);
            
            cart.clear();
            return "confirmation";
    }
	
	/*
	 * AJAX service
	 */
	@RequestMapping(value = "/validateMember", method = RequestMethod.POST)
	public @ResponseBody String validateMember(@RequestParam("email") String email) {
		Customer customer = customerService.getByEmail(email);
		if (customer != null) {
			return "true";
		}
		return "false";
	}
	
	@RequestMapping(value = "/getCartSize", method = RequestMethod.GET)
	public @ResponseBody String getCartSize() {
		return cart.sumQuantity().toString();
	}
	
}