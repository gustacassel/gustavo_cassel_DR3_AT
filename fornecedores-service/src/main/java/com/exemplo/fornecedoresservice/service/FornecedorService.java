package com.exemplo.fornecedoresservice.service;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    public List<Fornecedor> listarTodos() {
        return fornecedorRepository.findAll();
    }

    public Optional<Fornecedor> buscarPorId(Long id) {
        return fornecedorRepository.findById(id);
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        if (fornecedor.getNome() == null || fornecedor.getNome().isBlank()
                || fornecedor.getCnpj() == null || fornecedor.getCnpj().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e CNPJ sao obrigatorios");
        }

        if (fornecedorRepository.existsByCnpj(fornecedor.getCnpj())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe um fornecedor cadastrado com o CNPJ " + fornecedor.getCnpj());
        }

        // o id e gerado pelo banco, nunca aceito do corpo da requisicao
        fornecedor.setId(null);
        return fornecedorRepository.save(fornecedor);
    }
}
