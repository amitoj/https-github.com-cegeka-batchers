package be.cegeka.batchers.taxcalculator.presentation.rest;

import be.cegeka.batchers.taxcalculator.batch.api.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/runJob")
public class JobRestController {
    @Autowired
    JobService jobService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public void runJob() {
        System.out.println("Running job in rest controller");
        jobService.runTaxCalculatorJob();
    }

}