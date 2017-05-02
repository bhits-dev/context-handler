package gov.samhsa.c2s.contexthandler.service;

import ca.uhn.fhir.context.FhirContext;
import gov.samhsa.c2s.common.consentgen.ConsentDto;
import gov.samhsa.c2s.common.consentgen.ConsentGenException;
import gov.samhsa.c2s.common.consentgen.IndividualProviderDto;
import gov.samhsa.c2s.common.consentgen.OrganizationalProviderDto;
import gov.samhsa.c2s.common.log.Logger;
import gov.samhsa.c2s.common.log.LoggerFactory;
import gov.samhsa.c2s.contexthandler.service.dto.XacmlRequestDto;
import gov.samhsa.c2s.contexthandler.service.exception.MockFileReadException;
import gov.samhsa.c2s.contexthandler.service.exception.NoPolicyFoundException;
import gov.samhsa.c2s.contexthandler.service.exception.PolicyProviderException;
import gov.samhsa.c2s.common.consentgen.ConsentBuilder;

import org.codehaus.jackson.map.ObjectMapper;
import org.herasaf.xacml.core.policy.Evaluatable;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
public class FhirServerPolicyProviderImpl implements PolicyProvider {
    private static final String MOCK_FHIR_CONSENT_FILENAME = "mockFhirConsent.json";

    private final ConsentBuilder consentBuilder;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FhirContext fhirContext;

    @Autowired
    public FhirServerPolicyProviderImpl(ConsentBuilder consentBuilder, FhirContext fhirContext) {
        this.consentBuilder = consentBuilder;
        this.fhirContext = fhirContext;
    }

    @Override
    public List<Evaluatable> getPolicies(XacmlRequestDto xacmlRequest) throws NoPolicyFoundException, PolicyProviderException {
        ConsentDto consentDto;
        Consent fhirConsent = getMockFhirConsentObject();
        logger.info("FHIR CONSENT: " + fhirConsent.toString());

        try {
            consentDto = consentBuilder.buildFhirConsent2ConsentDto(fhirConsent);
            logger.info("Conversion of FHIR Consent to ConsentDto complete.");

            if(consentDto.getProvidersPermittedToDisclose().size() > 0) {
                logger.info("INDIVIDUAL FROM PROVIDER(S):");
                consentDto.getProvidersPermittedToDisclose()
                        .forEach(individualProviderDto -> logger.info(individualProviderDto.getNpi()));
            }

            if(consentDto.getOrganizationalProvidersPermittedToDisclose().size() > 0) {
                logger.info("ORGANIZATIONAL FROM PROVIDER(S):");
                consentDto.getOrganizationalProvidersPermittedToDisclose()
                        .forEach(organizationalProviderDto -> logger.info(organizationalProviderDto.getNpi()));
            }

        }catch (ConsentGenException e){
            logger.error("ConsentGenException occurred while trying to convert FHIR Consent object to ConsentDto object", e);
            throw new PolicyProviderException("Unable to process FHIR consent", e);
        }

        logger.info("CONSENT DTO OBJECT: " + consentDto.getConsentReferenceid() + "; " + consentDto.getConsentStart() + "; " + consentDto.getConsentEnd() + "; " + consentDto.getOrganizationalProvidersPermittedToDisclose().stream().findFirst().map(OrganizationalProviderDto::getNpi).orElse("") + "; " + consentDto.getProvidersPermittedToDisclose().stream().findFirst().map(IndividualProviderDto::getNpi).orElse(""));

        return null;
    }


    private Consent getMockFhirConsentObject(){
        ClassLoader classLoader = getClass().getClassLoader();
        ObjectMapper objectMapper = new ObjectMapper();
        File file;
        Consent fhirConsent;

        try{
            //noinspection ConstantConditions
            file = new File(classLoader.getResource(MOCK_FHIR_CONSENT_FILENAME).getFile());
        }catch(NullPointerException e){
            logger.error("Error getting mock file", e);
            throw new MockFileReadException(e);
        }

        try(FileReader fileReader = new FileReader(file)){
            fhirConsent = fhirContext.newJsonParser().parseResource(Consent.class, fileReader);
            logger.info("Mock FHIR consent successfully read from file and mapped to FHIR Consent object.");
        }catch(IOException e){
            logger.error("Error reading mock file from input stream", e);
            throw new MockFileReadException(e);
        }

        return fhirConsent;
    }

    //temp method
    @Override
    public Bundle tempGetFhirConsent(String mrn){

        return null;
    }

}
