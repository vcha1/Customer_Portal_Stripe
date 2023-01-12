package com.my1stle.customer.portal.web.controller.installation;

import com.my1stle.customer.portal.service.attachment.AttachmentData;
import com.my1stle.customer.portal.service.installation.detail.InstallationTimelineService;
import com.my1stle.customer.portal.service.installation.detail.InstallationDetailRetriever;
import com.my1stle.customer.portal.service.installation.detail.model.ContractDetail;
import com.my1stle.customer.portal.service.installation.detail.model.Timeline;
import com.my1stle.customer.portal.service.odoo.OdooInstallationData;
import com.my1stle.customer.portal.serviceImpl.adobesign.DefaultAdobeSign;
import com.my1stle.customer.portal.web.exception.ResourceNotFoundException;
import org.apache.tika.io.IOUtils;
import org.baeldung.persistence.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;

//import static com.my1stle.customer.portal.service.stripe.Server.test;

@Controller
public class DetailsController {
	private final InstallationDetailRetriever installationDetailRetriever;
	private final InstallationTimelineService installationTimelineService;

	//Testing Adobe
	private final DefaultAdobeSign defaultAdobeSign;

	@Autowired
	public DetailsController(
			InstallationDetailRetriever installationDetailRetriever,
			InstallationTimelineService installationTimelineService,
			DefaultAdobeSign defaultAdobeSign

	) {
		this.installationDetailRetriever = installationDetailRetriever;
		this.installationTimelineService = installationTimelineService;
		this.defaultAdobeSign = defaultAdobeSign;

	}

	@GetMapping("/installation/{id}/details")
	public String getInstallDetails(
			@AuthenticationPrincipal User user,
			Model model,
			@PathVariable("id") String installationId
	) throws ResourceNotFoundException {
		//InstallationDetail detail = installationDetailRetriever.retrieveAccessibleById(installationId);
		List<ContractDetail> adobeSign = defaultAdobeSign.getAgreementForInstallation(user.getEmail(), user.getFirstName(), user.getLastName());
		model.addAttribute("detail", adobeSign);
		List<String> signingUrls = defaultAdobeSign.getOpenAgreementForInstallation(user.getEmail(), user.getFirstName(), user.getLastName());
		model.addAttribute("signingUrls", signingUrls);

		Timeline timeline = installationTimelineService.getTimeline();
		OdooInstallationData odooData = new OdooInstallationData(user.getEmail(), installationId, "detail");
		model.addAttribute("odooData", odooData);
		model.addAttribute("timeline", timeline);

		//uncomment this after testing stripe
		return "installation/details";

		//return "serviceRequest/checkout/stripeclient.html";

	}

	@GetMapping(value = "/installation/{id}/files/STOP Package.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public @ResponseBody byte[] getInstallationSystemTurnOnPackage(
			@AuthenticationPrincipal User user,
			@PathVariable("id") String installationId
	) {
		return installationDetailRetriever.retrieveAccessibleSystemTurnOnPackageByInstallationId(installationId);
	}

	@GetMapping(value = "/installation/{id}/files/design-image", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getDesignImage(
			@AuthenticationPrincipal User user,
			@PathVariable("id") String installationId
	) {
		try {
			AttachmentData attachmentData = installationDetailRetriever.retrieveAccessibleInstallationDesignImageData(installationId);
			if (attachmentData != null) {
				return ResponseEntity.ok().contentType(MediaType.valueOf(attachmentData.getContentType())).body(attachmentData.getBody());
			}
		}
		catch (RuntimeException e) {
		}

		byte[] body;
		try {
			InputStream missingDesignImageStream = new ClassPathResource("static/img/installation/missing-design-picture.png").getInputStream();
			body = IOUtils.toByteArray(missingDesignImageStream);
		}
		catch (IOException e) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(body);
	}
}
