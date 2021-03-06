package be.cegeka.batchers.taxcalculator.batch.config;

import be.cegeka.batchers.taxcalculator.application.domain.Employee;
import be.cegeka.batchers.taxcalculator.application.service.exceptions.EmailSenderException;
import be.cegeka.batchers.taxcalculator.application.service.exceptions.TaxWebServiceNonFatalException;
import be.cegeka.batchers.taxcalculator.batch.config.listeners.CreateMonthlyTaxForEmployeeListener;
import be.cegeka.batchers.taxcalculator.batch.config.listeners.FailedStepStepExecutionListener;
import be.cegeka.batchers.taxcalculator.batch.config.listeners.SingleJVMJobProgressListener;
import be.cegeka.batchers.taxcalculator.batch.config.skippolicy.MaxConsecutiveExceptionsSkipPolicy;
import be.cegeka.batchers.taxcalculator.batch.domain.PayCheck;
import be.cegeka.batchers.taxcalculator.batch.domain.TaxCalculation;
import be.cegeka.batchers.taxcalculator.batch.processor.CalculateTaxProcessor;
import be.cegeka.batchers.taxcalculator.batch.processor.CallWebserviceProcessor;
import be.cegeka.batchers.taxcalculator.batch.processor.SendPaycheckProcessor;
import be.cegeka.batchers.taxcalculator.batch.tasklet.JobResultsTasklet;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.util.Arrays;

public abstract class AbstractEmployeeJobConfig extends DefaultBatchConfigurer {
    public static final String EMPLOYEE_JOB = "employeeJob";

    @Autowired
    protected DataSource dataSource;
    @Autowired
    protected StepBuilderFactory stepBuilders;

    /* ======== Readers & Writers ========*/
    @Autowired
    private JpaPagingItemReader<Employee> taxCalculatorItemReader;
    @Autowired
    private JpaItemWriter<TaxCalculation> taxCalculatorItemWriter;
    @Autowired
    private JpaPagingItemReader<TaxCalculation> wsCallItemReader;
    @Autowired
    private JpaItemWriter<PayCheck> wsCallItemWriter;

    /* ======== Processors ========*/
    @Autowired
    private CalculateTaxProcessor calculateTaxProcessor;
    @Autowired
    private CallWebserviceProcessor callWebserviceProcessor;
    @Autowired
    private SendPaycheckProcessor sendPaycheckProcessor;

    /* ======== Listeners ========*/
    @Autowired
    private FailedStepStepExecutionListener failedStepStepExecutionListener;
    @Autowired
    private MaxConsecutiveExceptionsSkipPolicy maxConsecutiveExceptionsSkipPolicy;
    @Autowired
    private CreateMonthlyTaxForEmployeeListener createMonthlyTaxForEmployeeListener;
    @Autowired
    private SingleJVMJobProgressListener singleJVMJobProgressListener;

    @Autowired
    private JobResultsTasklet jobResultsTasklet;

    @Autowired
    private TaskExecutor taskExecutor;

    protected Step taxCalculationStep(String stepName) {
        return stepBuilders
                .get(stepName)
                .<Employee, TaxCalculation>chunk(5)
                .reader(taxCalculatorItemReader)
                .processor(calculateTaxProcessor)
                .writer(taxCalculatorItemWriter)
                .taskExecutor(taskExecutor)
                .listener(taxCalculationStepProgressListener())
                .allowStartIfComplete(true)
                .build();
    }

    protected abstract Object taxCalculationStepProgressListener();

    protected Step wsCallAndGenerateAndSendPaycheckStep(String stepName) {
        CompositeItemProcessor<TaxCalculation, PayCheck> compositeItemProcessor = new CompositeItemProcessor<>();
        compositeItemProcessor.setDelegates(Arrays.asList(
                callWebserviceProcessor,
                sendPaycheckProcessor
        ));

        return stepBuilders.get(stepName)
                .<TaxCalculation, PayCheck>chunk(5)
                .faultTolerant()
                .skipPolicy(maxConsecutiveExceptionsSkipPolicy)
                .noRollback(TaxWebServiceNonFatalException.class)
                .noRollback(EmailSenderException.class)
                .reader(wsCallItemReader)
                .processor(compositeItemProcessor)
                .writer(wsCallItemWriter)
                .listener(createMonthlyTaxForEmployeeListener)
                .listener(maxConsecutiveExceptionsSkipPolicy)
                .listener(failedStepStepExecutionListener)
                .listener(singleJVMJobProgressListener)
                .allowStartIfComplete(true)
                .taskExecutor(taskExecutor)
                .build();
    }

    protected Step jobResultsPdf(String name) {
        return stepBuilders.get(name)
                .tasklet(jobResultsTasklet)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public JobExplorer jobExplorer() throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(dataSource);
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
