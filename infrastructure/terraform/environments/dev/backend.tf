terraform {
  backend "s3" {
    bucket         = "mini-ecommerce-tf-dv-state-eu-central-1"
    key            = "environments/dev/terraform.tfstate"
    region         = "eu-central-1"
    dynamodb_table = "mini-ecommerce-tf-locks"
    encrypt        = true
  }
}