-- Apenas banco local de desenvolvimento/teste. Não contém credenciais de banco.
-- psql -X -v ON_ERROR_STOP=1 -v login_bloqueado=a12.bloqueada.TOKEN -f PREPARAR-CONTA-BLOQUEADA.sql ...
-- Execute após concluir a pasta 02; a conta já deve ter autenticado com sucesso.
BEGIN;
SELECT set_config('aula12.login_bloqueado', :'login_bloqueado', true);
DO $$
DECLARE
    alvo RECORD;
BEGIN
    IF current_database() NOT IN ('instrumento2026_test', 'instrumento2026_dev') THEN
        RAISE EXCEPTION 'Preparação permitida somente nos bancos locais instrumento2026_test/dev';
    END IF;
    SELECT c.id, c.login, c.status, c.senha_hash, p.status AS pessoa_status, p.email
      INTO STRICT alvo FROM conta_acesso c JOIN pessoa p ON p.id = c.pessoa_id
     WHERE c.login = current_setting('aula12.login_bloqueado');
    IF alvo.login NOT LIKE 'a12.bloqueada.%'
       OR alvo.email NOT LIKE 'a12.bloqueada.%@example.com'
       OR alvo.pessoa_status <> 'ATIVO' OR alvo.status <> 'ATIVA'
       OR alvo.senha_hash NOT LIKE '{bcrypt}%' THEN
        RAISE EXCEPTION 'Fixture inválida: use somente a conta bloqueada criada na pasta 02 desta execução';
    END IF;
    UPDATE conta_acesso SET status = 'BLOQUEADA' WHERE id = alvo.id;
END $$;
COMMIT;
