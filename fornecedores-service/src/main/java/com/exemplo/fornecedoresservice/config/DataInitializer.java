package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FornecedorRepository fornecedorRepository;

    public DataInitializer(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Override
    public void run(String... args) {
        fornecedorRepository.save(new Fornecedor("Distribuidora Alfa Ltda", "11.222.333/0001-81"));
        fornecedorRepository.save(new Fornecedor("Tecno Suprimentos SA", "22.333.444/0001-50"));
        fornecedorRepository.save(new Fornecedor("Papelaria Central Ltda", "33.444.555/0001-20"));
        fornecedorRepository.save(new Fornecedor("Logistica Sul Transportes", "44.555.666/0001-03"));
        fornecedorRepository.save(new Fornecedor("Embalagens Norte Ltda", "55.666.777/0001-90"));
    }
}
