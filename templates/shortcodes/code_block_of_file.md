{% set data = load_data(path=page.colocated_path ~ path, format="plain") -%}
```{{info_string}}
{{ data }}
```
