package com.sequenomics.rdftotext.resources;

import com.sequenomics.rdftotext.domain.Rdf;
import com.sequenomics.rdftotext.services.RdfQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Controller
public class RootResource {

    private RdfQueryService rdfQueryService;

    public RootResource(RdfQueryService rdfQueryService) {
        this.rdfQueryService = rdfQueryService;
    }

    @GetMapping("/")
    public String rootPage(Model model) {
        model.addAttribute("rdf", new Rdf());
        return "main";
    }

    @PostMapping("/")
    public ModelAndView Covert(@ModelAttribute Rdf rdf, ModelAndView modelAndView) throws IOException {
        modelAndView.addObject("text", rdfQueryService.rdfToText(rdf.getContent()));
        modelAndView.setViewName("main");

        return modelAndView;
    }

}
